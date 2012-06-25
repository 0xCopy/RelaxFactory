package rxf.server;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.google.gson.JsonObject;
import rxf.server.CouchResultSet.tuple;
import rxf.server.CouchService.View;
import rxf.server.gen.CouchDriver;
import rxf.server.gen.CouchDriver.ViewFetch;

import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.driver.CouchMetaDriver.ETAG;

/**
 * Creates CouchService instances by translating {@literal @}View annotations into CouchDB design documents
 * and invoking them when the methods are called.
 */
public class CouchServiceFactory {
  public static <S extends CouchService<?>> S get(Class<S> clazz, String... ns) throws InterruptedException, ExecutionException {
    InvocationHandler handler = new CouchServiceHandler(clazz, ns);
    Class<?>[] interfaces = {clazz};
    return (S) Proxy.newProxyInstance(clazz.getClassLoader(), interfaces, handler);
  }

  /**
   * Actual generated instance for each proxy. This is designed to play nice with RequestFactory
   * and javascript (in the couch views) so uniqueness is by method _name_, not full signature.
   *
   * @param <E> type of entity that will be handled with this service proxy, used to be explicit about
   *            types in private members
   * @author colin
   */
  private static class CouchServiceHandler<E> implements InvocationHandler, CouchNamespace<E> {
    private Map<String, String> viewMethods = new HashMap<String, String>();
    private Future<Object> init;
    private Class<E> entityType;

    public CouchServiceHandler(Class<?> serviceInterface, String... ns) throws ExecutionException, InterruptedException {

      init(serviceInterface, ns);

    }

    private void init(final Class<?> serviceInterface, final String... initNs) throws ExecutionException, InterruptedException {

      Type[] genericInterfaces = serviceInterface.getGenericInterfaces();
      final ParameterizedType genericInterface = (ParameterizedType) genericInterfaces[0];
      init = EXECUTOR_SERVICE.submit(new Callable<Object>() {

        {
          entityType = (Class<E>) genericInterface.getActualTypeArguments()[0];
        }

        public Object call() throws Exception {
          for (int i = 0; i < initNs.length; i++) {
            String n = initNs[i];
            ns.values()[i].setMe(CouchServiceHandler.this, n);
          }
          try {
            //harvest, construct a view instance based on the interface. Probably not cheap, should be avoided.
            JsonObject design = new JsonObject();
            design.addProperty("language", "javascript");
            String id = "_design/" + getEntityName();

            design.addProperty("_id", id);

            JsonObject views = new JsonObject();

            Map<String, Type> returnTypes = new LinkedHashMap<String, Type>();

            for (Method m : serviceInterface.getMethods()) {
              String methodName = m.getName();
              returnTypes.put(methodName, m.getReturnType());//not sure if this is good enough
              View viewAnnotation = m.getAnnotation(View.class);
              if (null != viewAnnotation) {
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
            String doc = CouchDriver.DesignDocFetch.$().db(getPathPrefix()).designDocId(id).to().state(etag1).fire().json();

            if (null != doc) {
              //updating a doc, with a db but no rev or id? 
              CouchDriver.DocPersist.$().db(getPathPrefix())/*.id(id).rev(tx.rev())*/.validjson(design.toString()).to().fire().oneWay();
            } else {
              // this is nonsense, designdocs must be given an ID when created (via PUT) - we probably need a distinct
              // DesignDocPersist type
              CouchTx tx = CouchDriver.DocPersist.$().db(getPathPrefix())/*.designDocId(id)*/.validjson(design.toString()).to().fire().tx();
              System.out.println(tx);
            }

          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }

      });
    }


    public Object invoke(Object proxy, final Method method, final Object[] args) throws ExecutionException, InterruptedException {
      init.get();
      assert null != viewMethods;

      if (viewMethods.containsKey(method.getName())) {
        return EXECUTOR_SERVICE.submit(new Callable<Object>() {
          public Object call() throws Exception {

            String name = method.getName();
            /*       dont forget to uncomment this after new CouchResult gen*/
            if (viewMethods.containsKey(name)) {
              // where is the design doc defined? part of the view?
              CouchResultSet<E> rows = (CouchResultSet<E>) ViewFetch.$().db(getOrgName()).type(entityType).view(String.format(viewMethods.get(name), args)).to().fire().rows();
              if (null != rows && null != rows.rows) {
                ArrayList<E> ar = new ArrayList<E>();
                for (tuple<E> row : rows.rows) {
                  ar.add(row.value);
                }
                return ar;
              }
            }
            return null;
          }
        }).get();
      } else {
        //TODO: rewire implicit methods to be explicit wrappers?
        //persist or find by key
        if ("persist".equals(method.getName())) {
          //again, no point, see above with DocPersist
          return CouchDriver.DocPersist.$().db(getOrgName()).validjson(GSON.toJson(args[0])).to().fire().tx();
        } else {
          assert "find".equals(method.getName());
          String doc = CouchDriver.DocFetch.$().db(getOrgName()).docId((String) args[0]).to().fire().json();
          return (E) GSON.fromJson(doc, entityType);
        }
      }
    }


    ///CouchNS boilerplate

    private String entityName;

    //threadlocals dont help much.  rf is dispatched to new threads in a seperate executor.
    private String orgname;
    //slightly lazy
    private String pathPrefix;


    @Override
    public void setEntityName(String entityName) {
      this.entityName = entityName;
    }

    @Override
    public String getEntityName() {
      return null == entityName ? entityName = getDefaultEntityName() : entityName;
    }

    @Override
    public String getDefaultEntityName() {
      return getOrgName() + entityType.getSimpleName().toLowerCase();
    }

    @Override

    public String getOrgName() {
      return null == orgname ? orgname = BlobAntiPatternObject.getDefaultOrgName() : orgname;

    }

    public void setOrgname(String orgname) {
      this.orgname = orgname;
    }

    public String getPathPrefix() {
      return null == pathPrefix ? pathPrefix = '/' + getOrgName() + '/' + getEntityName() + '/' : pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
      this.pathPrefix = pathPrefix;
    }
  }
}
