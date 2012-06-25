package rxf.server;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import com.google.gson.annotations.SerializedName;
import rxf.server.CouchResultSet.tuple;
import rxf.server.CouchService.View;
import rxf.server.gen.CouchDriver;
import rxf.server.gen.CouchDriver.*;
import rxf.server.gen.CouchDriver.DocPersist.DocPersistTerminalBuilder;

import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.BlobAntiPatternObject.deepToString;
import static rxf.server.BlobAntiPatternObject.getDefaultOrgName;

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
    Map<String, String> viewMethods = new HashMap<String, String>();
    private Future<Object> init;
    Class<E> entityType;

    public CouchServiceHandler(Class<?> serviceInterface, String... ns) throws ExecutionException, InterruptedException {

      init(serviceInterface, ns);

    }

    public class CouchView {
      public String map;
      public String reduce;
    }

    public class CouchDesignDoc {
      @SerializedName("_id")
      public String id;
      @SerializedName("_rev")
      public String version;
      public String language = "javascript";


      public Map<String, CouchView> views = new LinkedHashMap<String, CouchView>();
    }

    ;

    private void init(final Class<?> serviceInterface, final String... initNs) throws ExecutionException, InterruptedException {

      Type[] genericInterfaces = serviceInterface.getGenericInterfaces();
      final ParameterizedType genericInterface = (ParameterizedType) genericInterfaces[0];
      entityType = (Class<E>) genericInterface.getActualTypeArguments()[0];
      init = EXECUTOR_SERVICE.submit(new Callable<Object>() {


        public Object call() throws Exception {
          for (int i = 0; i < initNs.length; i++) {
            String n = initNs[i];
            ns.values()[i].setMe(CouchServiceHandler.this, n);
          }
          try {
            //harvest, construct a view instance based on the interface. Probably not cheap, should be avoided.
            CouchDesignDoc design = new CouchDesignDoc();

            design.id = "_design/" + getEntityName();
            try {
              design.version = RevisionFetch.$().db(getPathPrefix()).docId(design.id).to().fire().json();
            } catch (Throwable ignored) {
              try {
                CouchTx tx = DbCreate.$().db(getPathPrefix()).to().fire().tx();
                System.err.println("had to create " + getPathPrefix());
              } finally {
              }
            }


            Map<String, Type> returnTypes = new LinkedHashMap<String, Type>();

            for (Method m : serviceInterface.getMethods()) {
              String methodName = m.getName();
              returnTypes.put(methodName, m.getReturnType());//not sure if this is good enough
              View viewAnnotation = m.getAnnotation(View.class);
              if (null != viewAnnotation) {
                CouchView view = new CouchView();
                if (!viewAnnotation.map().isEmpty())
                  view.map = viewAnnotation.map();
                if (!viewAnnotation.reduce().isEmpty()) {
                  view.reduce = viewAnnotation.reduce();
                }
                design.views.put(methodName, view);

                viewMethods.put(methodName, design.id + "/_view/" + methodName + "?key=\"%1$s\"");
              }

            }

            final String stringParam = GSON.toJson(design);
            DocPersistTerminalBuilder fire = DocPersist.$().db(getPathPrefix()).docId(design.id).validjson(stringParam).to().fire();
            CouchTx tx = fire.tx();
            System.err.println(deepToString(tx));

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
              assert entityType != null;
              CouchResultSet<E> rows = (CouchResultSet<E>) ViewFetch.$().db(getPathPrefix()).type(entityType).view(String.format(viewMethods.get(name), args)).to().fire().rows();
              if (null != rows && null != rows.rows) {
                List<E> ar = new ArrayList<E>();
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

        //persist or find by key
        if ("persist".equals(method.getName())) {
          //again, no point, see above with DocPersist
          String stringParam = GSON.toJson(args[0]);
          DocPersistTerminalBuilder fire = DocPersist.$().db(getPathPrefix()).validjson(stringParam).to().fire();
          CouchTx tx = fire.tx();
          return tx;
        } else {
          assert "find".equals(method.getName());
          String doc = CouchDriver.DocFetch.$().db(getPathPrefix()).docId((String) args[0]).to().fire().json();
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
      return /*getOrgName() +*/ entityType.getSimpleName().toLowerCase();
    }

    @Override

    public String getOrgName() {
      return null == orgname ? orgname = getDefaultOrgName() : orgname;

    }

    public void setOrgname(String orgname) {
      this.orgname = orgname;
    }

    public String getPathPrefix() {
      return null == pathPrefix ? pathPrefix = '/' + getOrgName() + getEntityName() + '/' : pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
      this.pathPrefix = pathPrefix;
    }


  }
}
