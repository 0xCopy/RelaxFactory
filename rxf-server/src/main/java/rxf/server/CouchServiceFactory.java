package rxf.server;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.*;

import com.google.gson.JsonObject;
import rxf.server.CouchDriver.*;
import rxf.server.CouchResultSet.tuple;
import rxf.server.CouchService.View;

import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.CouchMetaDriver.ETAG;

public class CouchServiceFactory {
  public static <S extends CouchService<?>> S get(Class<S> clazz) throws IOException, TimeoutException, InterruptedException {
    InvocationHandler handler = new CouchServiceHandler(clazz);
      Class<?>[] interfaces = {clazz};
      return ( S) Proxy.newProxyInstance(clazz.getClassLoader(), interfaces, handler);
  }

  /**
   * Actual generated instance for each proxy. This is designed to play nice with RequestFactory 
   * and javascript (in the couch views) so uniqueness is by method _name_, not full signature.
   * 
   * @author colin
   */
  private static class CouchServiceHandler implements InvocationHandler {
    private Map<String, String> viewMethods;
    Class<?> entityType;
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
      entityType = (Class<?>) ((ParameterizedType) serviceInterface.getGenericInterfaces()[0]).getActualTypeArguments()[0];
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
            CouchTx tx = new getRevisionBuilder().db(pathPrefix).docId(id).to().state(etag1).fire().tx();
            if (null != tx && Boolean.TRUE.equals(tx.ok())) {

              new updateDesignDocBuilder().db(pathPrefix).designDocId(id).rev(tx.rev()).validjson(design.toString()).to().fire().oneWay();
            } else {
              new createNewDesignDocBuilder().db(pathPrefix).designDocId(id).validjson(design.toString()).to().fire().oneWay();
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
      return EXECUTOR_SERVICE.submit(new Callable() {
        public Object call() throws Exception {

          String name = method.getName();
          Class<?> entityType1 = CouchServiceHandler.this.entityType;
          if (viewMethods.containsKey(name)) {
            CouchResultSet rows = new getViewBuilder<CouchService>().db(pathPrefix).view(String.format(viewMethods.get(name), args)).to().fire().rows();
            Class<?> entityType11 = entityType1;
            ArrayList ar = new ArrayList();
            for (Object row : rows.rows) {
              Object value = ((tuple) row).value;
              ar.add(GSON.fromJson(GSON.toJson(value), entityType11));
        }
            return ar;
          }
          return null;
        }
      }).get();
      }
    }
  }
