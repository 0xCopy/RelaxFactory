package rxf.server;

import org.jetbrains.annotations.NonNls;

public enum DbTerminal {
  /**
   * results are squashed.
   */
  oneWay {
    @Override
    public String builder(final CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit) {
      return "void " + name() + "(){\n    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable(){\n@Override\npublic void run(){\n" +
          "    try{\n    " +
          "rxf.server.CouchMetaDriver." + couchDriver + ".visit();\n}catch(Exception e){\n    e.printStackTrace();}\n    }\n    });\n}";
    }
  },
  /**
   * returns resultset from the assumed Future<couchTx>
   */
  rows {
    @Override
    public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit) {
      final String rt = "CouchResultSet<" + unit.getCanonicalName() + ">";
      return "\npublic " +
          rt +
          " " + name() + "(){ \n" +
          "try {\n" +
          "        return (" +
          rt +
          ") rxf.server.CouchMetaDriver." + couchDriver +
          ".visit();\n" +
          "      } catch (Exception e) {\n" +
          "        e.printStackTrace();  //todo: verify for a purpose \n" +
          "      };" +
          "        return null;};";
    }
  },
  /**
   * returns resultset from the assumed Future<couchTx>
   */
  pojo {
    @Override
    public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit) {
      final String rt = unit.getCanonicalName();
      return "\npublic " +
          rt +
          " " + name() + "(){ \n" +
          "try {\n" +
          "        return (" +
          rt +
          ") rxf.server.CouchMetaDriver." + couchDriver +
          ".visit();\n" +
          "      } catch (Exception e) {\n" +
          "        e.printStackTrace();  //todo: verify for a purpose \n" +
          "      };" +
          "        return null;};";
    }
  },
  /**
   * returns couchTx from the assumed Future<couchTx>
   */
  tx {
    @Override
    public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit) {
      return "public CouchTx tx(){try {\n" +
          "        return (" +
          CouchTx.class.getCanonicalName() +
          ") rxf.server.CouchMetaDriver." + couchDriver +
          ".visit();\n" +
          "      } catch (Exception e) {\n" +
          "        e.printStackTrace();  //todo: verify for a purpose \n" +
          "      };return null;};";
    }
  },
  /**
   * returns the Future<?> used.
   */
  future {
    @Override
    public String builder(final CouchMetaDriver couchDriver, DbKeys.etype[] parms, final Class unit) {
      final String rt = unit.getCanonicalName();
      final String canonicalName = rt;
      return
          "public Future<" + rt +
              ">future(){\n    try {\n\n    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<" + canonicalName +
              ">(){\n" +
              "@Override\n" +
              "public " +
              canonicalName + " call()throws Exception{\n" +
              "    return(" + canonicalName + ")rxf.server.CouchMetaDriver." + couchDriver +
              ".visit();\n" +
              "}\n" +
              "    " +
              "}\n" +
              "    );\n" +

              "    }catch(Exception e){e.printStackTrace();};" +
              "return null;};";
    }
  },
  /**
   * follows the _changes semantics with potential 1-byte chunksizes.
   */
  continuousFeed {
    @Override
    public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit) {
      return "public void " + name() + "(){" + BIG_EMPTY_PLACE + "};";
    }
  };
  @NonNls
  public static final String BIG_EMPTY_PLACE;

  static {                              //intellij bug in sub-parsers???
    BIG_EMPTY_PLACE = "throw new AbstractMethodError();";
  }

  public abstract String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit);


};
