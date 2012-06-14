package rxf.server;

import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.CouchMetaDriver.ETAG;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import rxf.server.CouchResultSet.tuple;
import rxf.server.CouchService.View;

import com.google.gson.JsonObject;

/**
 * Creates CouchService instances by translating {@literal @}View annotations into CouchDB design documents
 * and invoking them when the methods are called.
 *
 */
public class CouchServiceFactory {
  public static <S extends CouchService<?>> S get(Class<S> clazz) throws IOException, TimeoutException, InterruptedException {
    InvocationHandler handler = new CouchServiceHandler(clazz);
    Class<?>[] interfaces = {clazz};
    return (S) Proxy.newProxyInstance(clazz.getClassLoader(), interfaces, handler);
  }

  /**
   * Actual generated instance for each proxy. This is designed to play nice with RequestFactory 
   * and javascript (in the couch views) so uniqueness is by method _name_, not full signature.
   * 
   * @param <E> type of entity that will be handled with this service proxy, used to be explicit about
   * types in private members
   * @author colin
   */
  private static class CouchServiceHandler<E> implements InvocationHandler {
    private Map<String, String> viewMethods;
    private Class<E> entityType;
    private Future<Object> init;
    private String id;
    private String pathPrefix;

    public CouchServiceHandler(Class<?> serviceInterface) throws IOException, TimeoutException, InterruptedException {
      init(serviceInterface);

    }

    private void init(final Class<?> serviceInterface) throws ClosedChannelException, InterruptedException, TimeoutException {
      init = EXECUTOR_SERVICE.submit(new Callable<Object>() {
        public Object call() throws Exception {
          try {
            serviceInterface.getGenericInterfaces();
            entityType = (Class<E>) ((ParameterizedType) serviceInterface.getGenericInterfaces()[0]).getActualTypeArguments()[0];
            //harvest, construct a view instance based on the interface. Probably not cheap, should be avoided.
            JsonObject design = new JsonObject();
            design.addProperty("language", "javascript");
            String orgName = "rxf_";
            pathPrefix = orgName + entityType.getSimpleName().toLowerCase();
            id = "_design/" + orgName + "_" + pathPrefix;
            String viewPath = "/" + pathPrefix + "/" + id;
            design.addProperty("_id", id);

            JsonObject views = new JsonObject();

            viewMethods = new HashMap<String, String>();
            HashMap<String, Type> returnTypes = new HashMap<String, Type>();

            for (Method m : serviceInterface.getMethods()) {
              String methodName = m.getName();
              returnTypes.put(methodName, m.getReturnType());//not sure if this is good enough
              View viewAnnotation = m.getAnnotation(View.class);
              if (viewAnnotation != null) {
                JsonObject view = new JsonObject();
                view.addProperty("map", viewAnnotation.map());
                if (!"".equals(viewAnnotation.reduce())) {
                  view.addProperty("reduce", viewAnnotation.reduce());
                }
                views.add(methodName, view);

                viewMethods.put(methodName, id + "/_view/" + methodName + "?key=\"%1$s\"");
              }

            }
            design.add("views", views);
            Rfc822HeaderState etag1 = new Rfc822HeaderState(ETAG);
            CouchTx tx = CouchDriver.DesignDocFetch.$().db(pathPrefix).designDocId(id).to().state(etag1).fire().tx();
            if (null != tx && Boolean.TRUE.equals(tx.ok())) {
              //updating a doc, with a db but no rev or id? 
              CouchDriver.DocPersist.$().db(pathPrefix)/*.id(id).rev(tx.rev())*/.validjson(design.toString()).to().fire().oneWay();
            } else {
              // this is nonsense, designdocs must be given an ID when created (via PUT) - we probably need a distinct
              // DesignDocPersist type
              CouchDriver.DocPersist.$().db(pathPrefix)/*.designDocId(id)*/.validjson(design.toString()).to().fire().oneWay();
            }

          } catch (Exception e) {
            e.printStackTrace();  //todo: verify for a purpose
          }
          return null;
        }

      });
    }


    public Object invoke(Object proxy, final Method method, final Object[] args) throws ExecutionException, InterruptedException {
      init.get();
      assert viewMethods != null;
      
      if (viewMethods.containsKey(method.getName())) {
      return EXECUTOR_SERVICE.submit(new Callable<Object>() {
        public Object call() throws Exception {

          String name = method.getName();
          if (viewMethods.containsKey(name)) {
            // where is the design doc defined? part of the view?
            CouchResultSet<E> rows = CouchDriver.ViewFetch.<E>$().db(pathPrefix, entityType).view(String.format(viewMethods.get(name), args)).to().fire().rows();
            ArrayList<E> ar = new ArrayList<E>();
            for (tuple<E> row : rows.rows) {
              ar.add(row.value);
            }
            return ar;
          }
          return null;
        }
      }).get();
      } else {
        //persist or find by key
        if ("persist".equals(method.getName())) {
          //again, no point, see above with DocPersist
          
          return null;
        } else {
          assert "find".equals(method.getName());
          String doc = CouchDriver.DocFetch.$().db(pathPrefix).docId((String)args[0]).to().fire().pojo();
          return GSON.fromJson(doc, entityType);
        }
      }
    }
  }
}
