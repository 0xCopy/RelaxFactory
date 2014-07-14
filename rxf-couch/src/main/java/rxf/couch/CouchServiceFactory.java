package rxf.couch;

import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.Primitives;
import rxf.core.CouchNamespace;
import rxf.couch.CouchResultSet.tuple;
import rxf.couch.CouchService.CouchRequestParam;
import rxf.couch.CouchService.AttachmentsImpl;
import rxf.couch.CouchService.View;
import rxf.couch.driver.CouchMetaDriver;
import rxf.couch.gen.CouchDriver.*;
import rxf.couch.gen.CouchDriver.DocPersist.DocPersistActionBuilder;
import rxf.couch.gen.CouchDriver.DocPersist.DocPersistTerminalBuilder;
import rxf.couch.gen.CouchDriver.JsonSend.JsonSendTerminalBuilder;
import rxf.couch.gen.CouchDriver.ViewFetch.ViewFetchTerminalBuilder;
import rxf.rpc.RpcHelper;
import rxf.shared.CouchTx;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static rxf.web.inf.ProtocolMethodDispatch.deepToString;
import static rxf.rpc.RpcHelper.getDefaultOrgName;

/**
 * Creates CouchService instances by translating {@literal @}View annotations into CouchDB design documents and invoking
 * them when the methods are called.
 */
public class CouchServiceFactory {
  public static <S extends CouchService<?>> S get(Class<S> clazz, String... ns)
      throws InterruptedException, ExecutionException {
    InvocationHandler handler = new CouchServiceHandler(clazz, ns);
    Class<?>[] interfaces = {clazz};
    return (S) Proxy.newProxyInstance(clazz.getClassLoader(), interfaces, handler);
  }

  /**
   * Actual generated instance for each proxy. This is designed to play nice with RequestFactory and javascript (in the
   * couch views) so uniqueness is by method _name_, not full signature.
   * 
   * @param <E> type of entity that will be handled with this service proxy, used to be explicit about types in private
   *          members
   */
  private static class CouchServiceHandler<E> implements InvocationHandler, CouchNamespace {
    private final Class<E> entityType;
    private Map<String, String> viewMethods = null;
    private Future<Void> init;
    private String entityName;
    // threadlocals dont help much. rf is dispatched to new threads in a seperate executor.
    private String orgname;
    // slightly lazy
    private String pathPrefix;

    public CouchServiceHandler(Class<? extends CouchService<E>> serviceInterface, String... ns)
        throws ExecutionException, InterruptedException {
      Type[] genericInterfaces = serviceInterface.getGenericInterfaces();
      // TODO this assert might get tripped if we extend intermediate interfaces
      ParameterizedType genericInterface = (ParameterizedType) genericInterfaces[0];
      assert CouchService.class.isAssignableFrom((Class<?>) genericInterface.getRawType()) : genericInterface;

      entityType = (Class<E>) genericInterface.getActualTypeArguments()[0];

      init(serviceInterface, ns);
    }

    public void init(final Class<? extends CouchService<E>> serviceInterface,
        final String... initNs) throws ExecutionException, InterruptedException {
      init = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<Void>() {
        public Void call() throws Exception {
          for (int i = 0; i < initNs.length; i++) {
            String n = initNs[i];
            ns.values()[i].setMe(CouchServiceHandler.this, n);
          }
          try {
            // verify the DB exists
            ensureDbExists(getPathPrefix());

            // harvest, construct a view instance based per the interface. Probably not cheap, should be avoided.
            CouchDesignDoc design = new CouchDesignDoc();
            String designId = design.id = "_design/" + serviceInterface.getName();
            CouchDesignDoc existingDesignDoc = null;

            String pathPrefix1 = getPathPrefix();
            design.version = new RevisionFetch().db(pathPrefix1).docId(designId).to().fire().json();

            Map<String, Type> returnTypes = new TreeMap<String, Type>();
            viewMethods = new TreeMap<String, String>();
            for (Method m : serviceInterface.getMethods()) {
              String methodName = m.getName();
              returnTypes.put(methodName, m.getReturnType());// not sure if this is good enough
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

                // read the annotations from the service method
                Annotation[][] paramAnnotations = m.getParameterAnnotations();
                if (paramAnnotations.length == 1 && paramAnnotations[0].length == 0) {
                  // exactly one argument, and has no annotations?
                  // old, annotation-less queries
                  queryBuilder.append("key=%1$s");
                } else {
                  // else we assume sane, annotated parameters - if a param is not sane, skip
                  // TODO emit warning for useless params
                  Map<String, String> queryParams = new TreeMap<String, String>();
                  for (int i = 0; i < paramAnnotations.length; i++) {
                    // look for a CouchRequestParam per this param, if none, ignore
                    Annotation[] param = paramAnnotations[i];
                    for (int j = 0; j < param.length; j++) {// only the first param that fits
                      CouchRequestParam paramData =
                          param[j].annotationType().getAnnotation(CouchRequestParam.class);
                      if (paramData != null) {
                        queryParams.put(paramData.value(), "%" + (i + 1) + "$s");
                        break;
                      }
                    }
                  }
                  // after visiting args, check out the annotations per the method itself
                  Annotation[] methodAnnotations = m.getAnnotations();
                  for (Annotation a : methodAnnotations) {
                    CouchRequestParam paramData =
                        a.annotationType().getAnnotation(CouchRequestParam.class);
                    if (paramData != null) {
                      Object obj = a.annotationType().getMethod("value").invoke(a);
                      String val =
                          paramData.isJson() ? CouchMetaDriver.gson().toJson(obj) : obj + "";
                      queryParams.put(paramData.value(), URLEncoder.encode(val, "UTF-8"));
                    }
                  }
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

            // Now, before sending this new design doc, confirm that it isn't the same as the existing:
            if (!viewMethods.isEmpty()
                && (null == design.version || !design.equals(existingDesignDoc =
                    CouchMetaDriver.gson().fromJson(
                        new DocFetch().db(pathPrefix1).docId(designId).to().fire().json(),
                        CouchDesignDoc.class)))) {
              System.err.println("Existing design doc out of date, updating...");
              final String stringParam = CouchMetaDriver.gson().toJson(design);
              final JsonSendTerminalBuilder fire = new JsonSend().opaque(getPathPrefix())
              /* .docId(design.key) */.validjson(stringParam).to().fire();
              if (RpcHelper.DEBUG_SENDJSON) {
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
      String json = new DocFetch().db("").docId(dbName).to().fire().json();
      if (json == null) {
        // CouchTx tx = CouchDriver.GSON.fromJson(json, CouchTx.class);
        // if (tx.error() == null) {
        // Need to create the DB
        CouchTx tx = new DbCreate().db(getPathPrefix()).to().fire().tx();
        assert tx.ok() : tx.error();
        System.err.println("had to create " + dbName);
        return false;
      }
      return true;
    }

    // /CouchNS boilerplate

    public Object invoke(Object proxy, final Method method, final Object[] args)
        throws ExecutionException, InterruptedException {
      init.get();

      if (viewMethods.containsKey(method.getName())) {
        // view methods have several types they can returns, based per whether or not they use
        // reduce, if they return the key (simple or composite) as part of the data in a map
        // or just a list of data items, and how they return the data, as the full document,
        // or some simplified format.

        return RpcHelper.EXECUTOR_SERVICE.submit(new Callable<Object>() {
          public Object call() throws Exception {
            String name = method.getName();
            String[] jsonArgs = null;
            if (args != null) {// apparently args is null for a zero-arg method
              jsonArgs = new String[args.length];
              for (int i = 0; i < args.length; i++) {
                jsonArgs[i] = URLEncoder.encode(CouchMetaDriver.gson().toJson(args[i]), "UTF-8");
              }
            }
            Type keyType = Object.class;
            Type valueType;

            if (method.getGenericReturnType() instanceof Class) {
              // not generic, either just a simple object (reduce obj such as _stats) or primitive
              // read rows, unwrap to primitive/boxed/obj, return it
              if (method.getReturnType().isPrimitive()) {
                valueType = Primitives.wrap(method.getReturnType());
              } else {
                valueType = method.getReturnType();
              }
            } else {
              // assume list or map, parametrized type, else give up and use entityType
              ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
              if (returnType.getRawType() == Map.class) {
                Map map = new HashMap();
                // do map from assumed key type to assumed data type
                keyType = returnType.getActualTypeArguments()[0];
                valueType = returnType.getActualTypeArguments()[1];
              } else if (returnType.getRawType() == List.class) {
                valueType = returnType.getActualTypeArguments()[0];
              } else {
                // no idea, go with something somewhat sane
                valueType = entityType;
              }
            }

            /* dont forget to uncomment this after new CouchResult gen */
            final Map<String, String> stringStringMap = viewMethods;
            // Object[] cast to make varargs behave
            String format = String.format(stringStringMap.get(name), (Object[]) jsonArgs);
            final ViewFetchTerminalBuilder fire =
                new ViewFetch().db(getPathPrefix()).type(valueType).keyType(keyType).view(format)
                    .to().fire();
            CouchResultSet<?, ?> rows = fire.rows();

            if (method.getGenericReturnType() instanceof Class) {
              // not generic, either just a simple object (reduce obj such as _stats) or primitive
              // read rows, unwrap to primitive/boxed/obj, return it
              return rows.rows.get(0).value;
            } else {
              // assume list or map, parameterized type
              ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
              if (returnType.getRawType() == Map.class) {
                Map map = new HashMap();
                // populate map of specified type
                if (null != rows && null != rows.rows) {
                  for (tuple<?, ?> row : rows.rows) {
                    map.put(row.key, (E) row.value);
                  }
                }

                // return map
                return map;
              } else if (returnType.getRawType() == List.class) {
                List list = new ArrayList();
                // assume items in collection
                // iterate through items in rowset, populating list
                if (null != rows && null != rows.rows) {
                  List<E> ar = new ArrayList<E>();
                  for (tuple<?, ?> row : rows.rows) {
                    ar.add((E) row.value);
                  }
                  return ar;
                }
              }
            }
            return null;
          }
        }).get();
      } else {
        // persist or find by key
        if ("persist".equals(method.getName())) {
          // again, no point, see above with DocPersist
          String stringParam = CouchMetaDriver.gson().toJson(args[0]);
          final DocPersistActionBuilder to =
              new DocPersist().db(getPathPrefix()).validjson(stringParam).to();
          DocPersistTerminalBuilder fire = to.fire();
          CouchTx tx = fire.tx();
          return tx;
        } else if ("attachments".equals(method.getName())) {
          try {
            return new AttachmentsImpl(getPathPrefix(), (E) args[0]);
          } catch (NoSuchFieldException e) {
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          }
          return null;
        } else {
          assert "find" == (method.getName().intern());
          String doc =
              new DocFetch().db(getPathPrefix()).docId((String) args[0]).to().fire().json();
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
      return /* getOrgName() + */entityType.getSimpleName().toLowerCase();
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
