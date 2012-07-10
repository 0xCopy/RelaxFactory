package rxf.server;

import com.google.gson.annotations.SerializedName;
import rxf.server.CouchResultSet.tuple;
import rxf.server.CouchService.CouchRequestParam;
import rxf.server.CouchService.View;
import rxf.server.gen.CouchDriver;
import rxf.server.gen.CouchDriver.*;
import rxf.server.gen.CouchDriver.DocPersist.DocPersistActionBuilder;
import rxf.server.gen.CouchDriver.DocPersist.DocPersistTerminalBuilder;
import rxf.server.gen.CouchDriver.JsonSend.JsonSendTerminalBuilder;
import rxf.server.gen.CouchDriver.ViewFetch.ViewFetchTerminalBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static rxf.server.BlobAntiPatternObject.*;

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
        AtomicReference<Map<String, String>> viewMethods = new AtomicReference<Map<String, String>>(new TreeMap<String, String>());
        private Future init;
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


            public Map<String, CouchView> views = new TreeMap<String, CouchView>();
        }


        void init(final Class<?> serviceInterface, final String... initNs) throws ExecutionException, InterruptedException {

            Type[] genericInterfaces = serviceInterface.getGenericInterfaces();
            final ParameterizedType genericInterface = (ParameterizedType) genericInterfaces[0];
            entityType = (Class<E>) genericInterface.getActualTypeArguments()[0];
            init = EXECUTOR_SERVICE.submit(new Callable() {


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
                        }
                        if (null == design.version) {
                            DbCreate.$().db(getPathPrefix()).to().fire().oneWay();
                            System.err.println("had to create " + getPathPrefix());
                        }
                        Map<String, Type> returnTypes = new TreeMap<String, Type>();
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

                                StringBuilder queryBuilder = new StringBuilder(design.id).append("/_view/").append(methodName).append("?");

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
                                            CouchRequestParam paramData = param[j].annotationType().getAnnotation(CouchRequestParam.class);
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
                                        queryBuilder.append(URLEncoder.encode(param.getKey(), "UTF-8")).append("=").append(param.getValue()).append("&");
                                    }
                                }

                                viewMethods.get().put(methodName, queryBuilder.toString());
                            }
                        }
                        final String stringParam = GSON.toJson(design);
                        final JsonSendTerminalBuilder fire = JsonSend.$().opaque(getPathPrefix())/*.docId(design.id)*/.validjson(stringParam).to().fire();
                        final CouchTx tx = fire.tx();

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

            if (viewMethods.get().containsKey(method.getName())) {
                return EXECUTOR_SERVICE.submit(new Callable() {
                    public Object call() throws Exception {

                        String name = method.getName();
                        String[] jsonArgs = null;
                        if (args != null) {//apparently args is null for a zero-arg method
                            jsonArgs = new String[args.length];
                            for (int i = 0; i < args.length; i++) {
                                jsonArgs[i] = URLEncoder.encode(GSON.toJson(args[i]), "UTF-8");
                            }
                        }
                        /*       dont forget to uncomment this after new CouchResult gen*/
                        final Map<String, String> stringStringMap = viewMethods.get();
                        //Object[] cast to make varargs behave
                        String format = String.format(stringStringMap.get(name), (Object[]) jsonArgs);
                        final ViewFetchTerminalBuilder fire = ViewFetch.$().db(getPathPrefix()).type(entityType).view(format).to().fire();
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
                    String stringParam = GSON.toJson(args[0]);
                    final DocPersistActionBuilder to = DocPersist.$().db(getPathPrefix()).validjson(stringParam).to();
                    DocPersistTerminalBuilder fire = to.fire();
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


        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }


        public String getEntityName() {
            return null == entityName ? entityName = getDefaultEntityName() : entityName;
        }


        public String getDefaultEntityName() {
            return /*getOrgName() +*/ entityType.getSimpleName().toLowerCase();
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


    }
}
