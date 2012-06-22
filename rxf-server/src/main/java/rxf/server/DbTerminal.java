package rxf.server;

import java.nio.ByteBuffer;

import org.intellij.lang.annotations.Language;
import rxf.server.an.DbKeys.etype;
import rxf.server.driver.CouchMetaDriver;

public enum DbTerminal {
  /**
   * results are squashed.
   */
  oneWay {
    @Override
    public String builder(CouchMetaDriver couchDriver, etype[] parms, boolean implementation) {
      return (implementation ? "public " : "") + "void " + name() + "()" + (implementation ? "{\n    final DbKeysBuilder<Object>dbKeysBuilder=(DbKeysBuilder<Object>)DbKeysBuilder.get();\n" +
          "final ActionBuilder<Object>actionBuilder=(ActionBuilder<Object>)ActionBuilder.get();" +
          "\ndbKeysBuilder.validate();\n BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable(){" +
          "@Override\npublic void run(){\n" + "    try{\n\n      DbKeysBuilder.currentKeys.set(dbKeysBuilder);   \n      ActionBuilder.currentAction.set(actionBuilder); \nrxf.server.driver.CouchMetaDriver." + couchDriver + ".visit(/*dbKeysBuilder,actionBuilder*/);\n}catch(Exception e){\n    e.printStackTrace();}\n    }\n    });\n}" : ";");
    }
  },
  /**
   * returns resultset from the assumed Future<couchTx>
   */
  rows {
    @Override
    public String builder(CouchMetaDriver couchDriver, etype[] parms, boolean implementation) {
      String unit= ByteBuffer.class.getCanonicalName();int typeParamsStart = "ByteBuffer".indexOf('<');
      String fqsn = typeParamsStart == -1 ? unit : unit.substring(0, typeParamsStart);

      final String cmdName = "rxf.server.driver.CouchMetaDriver." + couchDriver;
      final String s = "{\n    try{\n    return(" + unit + ")" +
          "BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(" +
          cmdName
          + ".visit()).toString(),\n    new java.lang.reflect.ParameterizedType(){\npublic java.lang.reflect.Type getRawType(){\n    return " + fqsn + ".class;}\npublic java.lang.reflect.Type getOwnerType(){\n    return null;}\npublic java.lang.reflect.Type[]getActualTypeArguments(){\n    return new java.lang.reflect.Type[]{" +
          couchDriver.name() + "   .this.<Class>get( DbKeys.etype.type)};}});\n}catch(Exception e){\n    e.printStackTrace();\n}\n    return null;}";
      return (implementation ? "public " : "") + unit + " " + name() + "()" + (implementation ? s : ";");
    }
  },
  /**
   * returns resultset from the assumed Future<couchTx>
   */
  pojo {
    @Override
    public String builder(CouchMetaDriver couchDriver, etype[] parms, boolean implementation) {
      return (implementation ? " public " : "") +
          ByteBuffer.class.getCanonicalName() +
          " " + name() + "()" + (implementation ?
          "{ \n" +
              "try {\n" +
              "        return (" +
              ByteBuffer.class.getCanonicalName() +
              ") rxf.server.driver.CouchMetaDriver." + couchDriver +
              ".visit();\n" +
              "      } catch (Exception e) {\n" +
              "        e.printStackTrace();   \n" +
              "      } " +
              "        return null;} " : ";");
    }
  },
  /**
   * returns couchTx from the assumed Future<couchTx>
   */
  tx {
    @Override
    public String builder(CouchMetaDriver couchDriver, etype[] parms, boolean implementation) {
      return (implementation ? " public " : "") + " CouchTx tx()" + (implementation ?
          "{try {\n" +
              "        return (CouchTx)rxf.server.BlobAntiPatternObject.GSON.fromJson(String.valueOf(" +
              " rxf.server.driver.CouchMetaDriver." + couchDriver +
              ".visit()),CouchTx.class);\n" +
              "      } catch (Exception e) {\n" +
              "        e.printStackTrace();   \n" +
              "      } return null;} " : ";");
    }
  },
  /**
   * returns the Future<?> used.
   */
  future {
    @Override

    public String builder(CouchMetaDriver couchDriver, etype[] parms, boolean implementation) {
      return
          (implementation ? "public " : "") + "Future<ByteBuffer>future()" +
              (implementation ? "{\n    try{\n    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>(){\nfinal DbKeysBuilder dbKeysBuilder=(DbKeysBuilder)DbKeysBuilder.get();\n" +
                  "final ActionBuilder actionBuilder=(ActionBuilder)ActionBuilder.get();\n"
                  +
                  "public " + ByteBuffer.class.getCanonicalName() + " call() throws Exception{" +
                  "    DbKeysBuilder.currentKeys.set(dbKeysBuilder);" +
                  "\nActionBuilder.currentAction.set(actionBuilder);\n" +
                  "return(" + ByteBuffer.class.getCanonicalName() + ")rxf.server.driver.CouchMetaDriver." + couchDriver + ".visit(dbKeysBuilder,actionBuilder);\n}" +
                  "\n    }" +
                  "\n    );" +
                  "\n} catch(Exception e){\n    e.printStackTrace();\n}\n    return null;\n}" : ";");
    }
  },
  /**
   * follows the _changes semantics with potential 1-byte chunksizes.
   */
  continuousFeed {
    @Override
    public String builder(CouchMetaDriver couchDriver, etype[] parms, boolean implementation) {
      return (implementation ? " public " : "") + " void " + name() + "()" + (implementation ? ("{" + BIG_EMPTY_PLACE + "} ") : ";\n");
    }
  }, json {
    @Override
    public String builder(CouchMetaDriver couchDriver, etype[] parms, boolean implementation) {
      @Language("JAVA") String s = "{try{\n    return one.xio.HttpMethod.UTF8.decode(avoidStarvation(CouchMetaDriver." +
          "" + couchDriver + ".visit())).toString();\n}catch(Exception e){\n    e.printStackTrace();  \n}\n    return null;\n}";
      return (implementation ? " public " : "") + " String  json()" + (implementation ?
          s : ";");
    }
  };
  public static final String BIG_EMPTY_PLACE = "throw new AbstractMethodError();";

  public abstract String builder(CouchMetaDriver couchDriver, etype[] parms, boolean implementation);
};
