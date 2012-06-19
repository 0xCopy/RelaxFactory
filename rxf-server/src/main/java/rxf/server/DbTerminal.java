package rxf.server;

import rxf.server.an.DbKeys;
import rxf.server.driver.CouchMetaDriver;

public enum DbTerminal {
  /**
   * results are squashed.
   */
  oneWay {
    @Override
    public String builder(final CouchMetaDriver couchDriver, DbKeys.etype[] parms, String unit, boolean implementation) {
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
    public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, String unit, boolean implementation) {
      int typeParamsStart = unit.indexOf('<');
      String fqsn = typeParamsStart == -1 ? unit : unit.substring(0, typeParamsStart);

      final String cmdName = "rxf.server.driver.CouchMetaDriver." + couchDriver;
      final String s = "{\n    try{\n    return(" + unit + ")" +
          "BlobAntiPatternObject.GSON.fromJson((String)" +
          cmdName
          + ".visit(),\n    new java.lang.reflect.ParameterizedType(){\npublic java.lang.reflect.Type getRawType(){\n    return " + fqsn + ".class;}\npublic java.lang.reflect.Type getOwnerType(){\n    return null;}\npublic java.lang.reflect.Type[]getActualTypeArguments(){\n    return new java.lang.reflect.Type[]{" +
          couchDriver.name() + "   .this.<Class>get( DbKeys.etype.type)};}});\n}catch(Exception e){\n    e.printStackTrace();\n}\n    return null;}";
      return (implementation ? "public " : "") + unit + " " + name() + "()" + (implementation ? s : ";");
    }
  },
  /**
   * returns resultset from the assumed Future<couchTx>
   */
  pojo {
    @Override
    public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, String unit, boolean implementation) {
      return (implementation ? " public " : "") +
          unit +
          " " + name() + "()" + (implementation ?
          "{ \n" +
              "try {\n" +
              "        return (" +
              unit +
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
    public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, String unit, boolean implementation) {
      return (implementation ? " public " : "") + " CouchTx tx()" + (implementation ?
          "{try {\n" +
              "        return (" +
              CouchTx.class.getCanonicalName() +
              ") rxf.server.driver.CouchMetaDriver." + couchDriver +
              ".visit();\n" +
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

    public String builder(final CouchMetaDriver couchDriver, DbKeys.etype[] parms, final String unit, boolean implementation) {
      return
          (implementation ? "public " : "") + "Future<" + unit +
              ">future()" +
              (implementation ? "{\n    try{\n    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<" + unit +
                  ">(){\n\n\n  final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();\n" +
                  "final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();\n\n"
                  +
                  "public " +
                  unit + " call()throws Exception{ \n        \n" +
                  "                    DbKeysBuilder.currentKeys.set(dbKeysBuilder);  \n" +
                  " ActionBuilder.currentAction.set(actionBuilder);  " +
                  "return(" + unit + ")rxf.server.driver.CouchMetaDriver." + couchDriver +
                  ".visit(dbKeysBuilder,actionBuilder);}});\n}catch(Exception e){e.printStackTrace();}return null;}" : ";");
    }
  },
  /**
   * follows the _changes semantics with potential 1-byte chunksizes.
   */
  continuousFeed {
    @Override
    public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, String unit, boolean implementation) {
      return (implementation ? " public " : "") + " void " + name() + "()" + (implementation ? ("{" + BIG_EMPTY_PLACE + "} ") : ";\n");
    }
  };
  public static final String BIG_EMPTY_PLACE = "throw new AbstractMethodError();";

  public abstract String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, String unit, boolean implementation);


};
