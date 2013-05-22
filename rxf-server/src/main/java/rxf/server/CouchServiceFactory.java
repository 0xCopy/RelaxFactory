package rxf.server;

import com.google.gson.annotations.SerializedName;
import rxf.server.CouchResultSet.tuple;
import rxf.server.CouchService.CouchRequestParam;
import rxf.server.CouchService.View;
import rxf.server.driver.CouchMetaDriver;
import rxf.server.gen.CouchDriver.*;
import rxf.server.gen.CouchDriver.DocPersist.DocPersistActionBuilder;
import rxf.server.gen.CouchDriver.DocPersist.DocPersistTerminalBuilder;
import rxf.server.gen.CouchDriver.JsonSend.JsonSendTerminalBuilder;
import rxf.server.gen.CouchDriver.ViewFetch.ViewFetchTerminalBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static rxf.server.BlobAntiPatternObject.deepToString;
import static rxf.server.BlobAntiPatternObject.getDefaultOrgName;

/**
 * Creates CouchService instances by translating {@literal @}View annotations into CouchDB design documents
 * and invoking them when the methods are called.
 */
public class CouchServiceFactory {
  public static <S extends CouchService<?>> S get(Class<S> clazz, String... ns)
      throws InterruptedException, ExecutionException {
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
   */
  private static class CouchServiceHandler<E> implements InvocationHandler, CouchNamespace {
    private final Class<E> entityType;
    private Map<String, String> viewMethods = null;
    private Future<Void> init;
    private String entityName;
    //threadlocals dont help much.  rf is dispatched to new threads in a seperate executor.
    private String orgname;
    //slightly lazy
    private String pathPrefix;

    public CouchServiceHandler(Class<? extends CouchService<E>> serviceInterface, String... ns)
        throws ExecutionException, InterruptedException {
      Type[] genericInterfaces = serviceInterface.getGenericInterfaces();
      //TODO this assert might get tripped if we extend intermediate interfaces
      ParameterizedType genericInterface = (ParameterizedType) genericInterfaces[0];
      assert CouchService.class.isAssignableFrom((Class<?>) genericInterface.getRawType()) : genericInterface;

      entityType = (Class<E>) genericInterface.getActualTypeArguments()[0];

      init(serviceInterface, ns);
    }

    public void init(final Class<? extends CouchService<E>> serviceInterface,
        final String... initNs) throws ExecutionException, InterruptedException {
      init = BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<Void>() {
        public Void call() throws Exception {
          for (int i = 0; i < initNs.length; i++) {
            String n = initNs[i];
            ns.values()[i].setMe(CouchServiceHandler.this, n);
          }
          try {
            //verify the DB exists
            ensureDbExists(getPathPrefix());

            //harvest, construct a view instance based on the interface. Probably not cheap, should be avoided.
            CouchDesignDoc design = new CouchDesignDoc();
            String designId = design.id = "_design/" + getEntityName();
            CouchDesignDoc existingDesignDoc = null;

            String pathPrefix1 = getPathPrefix();
            design.version = RevisionFetch.$().db(pathPrefix1).docId(designId).to().fire().json();

            Map<String, Type> returnTypes = new TreeMap<String, Type>();
            viewMethods = new TreeMap<String, String>();
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

                StringBuilder queryBuilder =
                    new StringBuilder(designId).append("/_view/").append(methodName).append("?");

                Annotation[][] paramAnnotations = m.getParameterAnnotations();
                if (paramAnnotations.length == 1 && paramAnnotations[0].length == 0) {
                  //old, annotation-less queries
                  queryBuilder.append("key=%1$s");
                } else {
                  Map<String, String> queryParams = new TreeMap<String, String>();
                  for (int i = 0; i < paramAnnotations.length; i++) {
                    // look for a CouchRequestParam on this param, if none, ignore
                    Annotation[] param = paramAnnotations[i];
                    for (int j = 0; j < param.length; j++) {//only the first param that fits
                      CouchRequestParam paramData =
                          param[j].annotationType().getAnnotation(CouchRequestParam.class);
                      if (paramData != null) {
                        queryParams.put(paramData.value(), "%" + (i + 1) + "$s");
                        break;
                      }
                    }
                  }
                  //not supporting method annotations yet, unsure how to address a.value()
                  //                  Annotation[] methodAnnotations = m.getAnnotations();
                  //                  for (Annotation a : methodAnnotations) {
                  //                    CouchRequestParam paramData = a.annotationType().getAnnotation(CouchRequestParam.class);
                  //                    if (paramData != null) {
                  //                      //probably should kick this through GSON...
                  //                      queryParams.put(paramData.value(), URLEncoder.encode("" + a.value(), "UTF-8"));
                  //                    }
                  //                  }
                  for (Map.Entry<String, String> param : queryParams.entrySet()) {
                    // write out key = value
                    // note that key is encoded, value is dealt with when the value is created
                    queryBuilder.append(URLEncoder.encode(param.getKey(), "UTF-8")).append("=")
                        .append(param.getValue()).append("&");
                  }
                }

                viewMethods.put(methodName, queryBuilder.toString());
              }
            }
            viewMethods = Collections.unmodifiableMap(viewMethods);

            //Now, before sending this new design doc, confirm that it isn't the same as the existing:
            if (!viewMethods.isEmpty()
                && (null == design.version || !design.equals(existingDesignDoc =
                    CouchMetaDriver.gson().fromJson(
                        DocFetch.$().db(pathPrefix1).docId(designId).to().fire().json(),
                        CouchDesignDoc.class)))) {
              System.err.println("Existing design doc out of date, updating...");
              final String stringParam = CouchMetaDriver.gson().toJson(design);
              final JsonSendTerminalBuilder fire = JsonSend.$().opaque(getPathPrefix())
              /*.docId(design.key)*/.validjson(stringParam).to().fire();
              if (BlobAntiPatternObject.DEBUG_SENDJSON) {
                CouchTx tx = fire.tx();
                assert tx.ok() : tx.error();
                System.err.println(deepToString(tx));
              } else {
                fire.future().get();
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }
      });

    }

    private boolean ensureDbExists(String dbName) {
      String json = DocFetch.$().db("").docId(dbName).to().fire().json();
      if (json == null) {
        //			CouchTx tx = CouchDriver.GSON.fromJson(json, CouchTx.class);
        //			if (tx.error() == null) {
        // Need to create the DB
        CouchTx tx = DbCreate.$().db(getPathPrefix()).to().fire().tx();
        assert tx.ok() : tx.error();
        System.err.println("had to create " + dbName);
        return false;
      }
      return true;
    }

    ///CouchNS boilerplate

    public Object invoke(Object proxy, final Method method, final Object[] args)
        throws ExecutionException, InterruptedException {
      init.get();

      if (viewMethods.containsKey(method.getName())) {
        return BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<Object>() {
          public Object call() throws Exception {

            String name = method.getName();
            String[] jsonArgs = null;
            if (args != null) {//apparently args is null for a zero-arg method
              jsonArgs = new String[args.length];
              for (int i = 0; i < args.length; i++) {
                jsonArgs[i] = URLEncoder.encode(CouchMetaDriver.gson().toJson(args[i]), "UTF-8");
              }
            }
            /*       dont forget to uncomment this after new CouchResult gen*/
            final Map<String, String> stringStringMap = viewMethods;
            //Object[] cast to make varargs behave
            String format = String.format(stringStringMap.get(name), (Object[]) jsonArgs);
            final ViewFetchTerminalBuilder fire =
                ViewFetch.$().db(getPathPrefix()).type(entityType).view(format).to().fire();
            CouchResultSet<E> rows = (CouchResultSet<E>) fire.rows();
            if (null != rows && null != rows.rows) {
              List<E> ar = new ArrayList<E>();
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
          String stringParam = CouchMetaDriver.gson().toJson(args[0]);
          final DocPersistActionBuilder to =
              DocPersist.$().db(getPathPrefix()).validjson(stringParam).to();
          DocPersistTerminalBuilder fire = to.fire();
          CouchTx tx = fire.tx();
          return tx;
        } else {
          assert "find" == (method.getName().intern());
          String doc = DocFetch.$().db(getPathPrefix()).docId((String) args[0]).to().fire().json();
          return (E) CouchMetaDriver.gson().fromJson(doc, entityType);
        }
      }
    }

    public String getEntityName() {
      return null == entityName ? entityName = getDefaultEntityName() : entityName;
    }

    public void setEntityName(String entityName) {
      this.entityName = entityName;
    }

    public String getDefaultEntityName() {
      return /*getOrgName() +*/entityType.getSimpleName().toLowerCase();
    }

    public String getOrgName() {
      return null == orgname ? orgname = getDefaultOrgName() : orgname;

    }

    public void setOrgname(String orgname) {
      this.orgname = orgname;
    }

    public String getPathPrefix() {
      return null == pathPrefix ? pathPrefix = getOrgName() + getEntityName() : pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
      this.pathPrefix = pathPrefix;
    }

    public final static class CouchView {
      public String map;
      public String reduce;

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof CouchView)) {
          return false;
        }
        CouchView other = (CouchView) obj;
        return ((map == null && other.map == null) || map.equals(other.map))
            && ((reduce == null && other.reduce == null || reduce.equals(other.reduce)));
      }
    }

    public final static class CouchDesignDoc {
      @SerializedName("_id")
      public String id;
      @SerializedName("_rev")
      public String version;
      public String language = "javascript";
      public Map<String, CouchView> views = new TreeMap<String, CouchView>();

      @Override
      public boolean equals(Object obj) {
        if (!(obj instanceof CouchDesignDoc)) {
          return false;
        }
        CouchDesignDoc other = (CouchDesignDoc) obj;
        return id.equals(other.id) && views.equals(other.views);
      }
    }

  }
}
