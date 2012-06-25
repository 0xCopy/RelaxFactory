package rxf.server;

import java.nio.ByteBuffer;
import java.util.EnumSet;

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


      String visitor = "rxf.server.driver.CouchMetaDriver." + couchDriver;
      String cmdName = couchDriver.name();
      @Language("JAVA") String s = "{\n" +
          "            try {\n" +
          "              return    BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(" +
          visitor + ".visit()).toString(),\n" +
          "                  new ParameterizedType() {\n" +
          "                    public Type getRawType() {\n" +
          "                      return CouchResultSet.class;\n" +
          "                    }\n" +
          "\n" +
          "                    public Type getOwnerType() {\n" +
          "                      return null;\n" +
          "                    }\n" +
          "\n" +
          "                    public Type[] getActualTypeArguments() {\n" +
          "                      return new Type[]{" +
          cmdName + ".this.<Class>get(DbKeys.etype.type)};\n" +
          "                    }\n" +
          "                  });\n" +
          "            } catch (Exception e) {\n" +
          "              e.printStackTrace();\n" +
          "            }\n" +
          "            return null;\n" +
          "          }";
      return (implementation ? "public " : "") + CouchResultSet.class.getCanonicalName() + " " + name() + "()" + (implementation ? s : ";");
    }
  },
  /**
   * returns resultset from the assumed Future<couchTx>
   */
  pojo {
    @Override
    public String builder(CouchMetaDriver couchDriver, etype[] parms, boolean implementation) {
      EnumSet<etype> of = EnumSet.of(parms[0], parms);
      assert of.contains(etype.type);

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
              "        return (CouchTx)rxf.server.BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(" +
              " rxf.server.driver.CouchMetaDriver." + couchDriver + ".visit()).toString(),CouchTx.class);\n" +
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
      @Language("JAVA") String s = "   try {\n" +
          "              ByteBuffer visit = CouchMetaDriver." + couchDriver.name() +
          ".visit();\n" +
          "              return null==visit?null:HttpMethod.UTF8.decode(avoidStarvation(visit)).toString();\n" +
          "            } catch (Exception e) {\n" +
          "              e.printStackTrace();\n" +
          "            }\n" +
          "            return null;";
      return (implementation ? " public " : "") + " String  json()" + (implementation ?
          s : ";");
    }
  };
  public static final String BIG_EMPTY_PLACE = "throw new AbstractMethodError();";

  public abstract String builder(CouchMetaDriver couchDriver, etype[] parms, boolean implementation);
};
