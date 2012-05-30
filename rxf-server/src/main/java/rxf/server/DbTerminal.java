package rxf.server;

public enum DbTerminal {
    /**
     * results are squashed.
     */
    oneWay {
        @Override
        public String builder(final CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit, boolean implementation) {
            return (implementation ? " public " : "") + "void " + name() + "()" + (implementation ?
                    "{\n    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable(){\n@Override\npublic void run(){\n" +
                            "    try{\n    rxf.server.CouchMetaDriver." + couchDriver + ".visit();\n}catch(Exception e){\n    e.printStackTrace();}\n    }\n    });\n}" : ";");
        }
    },
    /**
     * returns resultset from the assumed Future<couchTx>
     */
    rows {
        @Override
        public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit, boolean implementation) {
            final String rt = "CouchResultSet<" + unit.getCanonicalName() + ">";
            return (implementation ? " public " : "") +
                    rt +
                    " " + name() + "()" + (implementation ?
                    "{ \n" +
                            "try {\n" +
                            "        return (" +
                            rt +
                            ") rxf.server.CouchMetaDriver." + couchDriver +
                            ".visit();\n" +
                            "      } catch (Exception e) {\n" +
                            "        e.printStackTrace();    \n" +
                            "      } " +
                            "        \nreturn null ;}" : ";");
        }
    },
    /**
     * returns resultset from the assumed Future<couchTx>
     */
    pojo {
        @Override
        public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit, boolean implementation) {
            final String rt = unit.getCanonicalName();
            return (implementation ? " public " : "") +
                    rt +
                    " " + name() + "()" + (implementation ?
                    "{ \n" +
                            "try {\n" +
                            "        return (" +
                            rt +
                            ") rxf.server.CouchMetaDriver." + couchDriver +
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
        public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit, boolean implementation) {
            return (implementation ? " public " : "") + " CouchTx tx()" + (implementation ?
                    "{try {\n" +
                            "        return (" +
                            CouchTx.class.getCanonicalName() +
                            ") rxf.server.CouchMetaDriver." + couchDriver +
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
        public String buildAction(final CouchMetaDriver couchDriver, DbKeys.etype[] parms, final Class unit, boolean implementation) {
            return "";
        }

        @Override

        public String builder(final CouchMetaDriver couchDriver, DbKeys.etype[] parms, final Class unit, boolean implementation) {
            final String rt = unit.getCanonicalName();
            final String canonicalName = rt;
            return
                    (implementation ? " public " : "") + " Future<" + rt +
                            ">future()" +
                            (implementation ? "{\n    try {\n\n    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<" + canonicalName +
                                    ">(){\n"
                                    +
                                    "public " +
                                    canonicalName + " call()throws Exception{\n" +
                                    "    return(" + canonicalName + ")rxf.server.CouchMetaDriver." + couchDriver +
                                    ".visit();}});\n    }catch(Exception e){e.printStackTrace();} return null;} " : ";");
        }
    },
    /**
     * follows the _changes semantics with potential 1-byte chunksizes.
     */
    continuousFeed {
        @Override
        public String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit, boolean implementation) {
            return (implementation ? " public " : "") + " void " + name() + "()" + (implementation ? ("{" + BIG_EMPTY_PLACE + "} ") : ";\n");
        }
    };
    public static final String BIG_EMPTY_PLACE = "throw new AbstractMethodError();";

    public abstract String builder(CouchMetaDriver couchDriver, DbKeys.etype[] parms, Class unit, boolean implementation);


};
