//"C:\Program Files\Java\jdk1.7.0_01\bin\java" -Didea.launcher.port=7532 "-Didea.launcher.bin.path=C:\Program Files (x86)\JetBrains\IntelliJ IDEA 10.5.2\bin" -Dfile.encoding=UTF-8 -classpath "C:\Program Files\Java\jdk1.7.0_01\jre\lib\alt-rt.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\charsets.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\deploy.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\javaws.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\jce.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\jsse.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\management-agent.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\plugin.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\resources.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\rt.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\ext\dnsns.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\ext\localedata.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\ext\sunec.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\ext\sunjce_provider.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\ext\sunmscapi.jar;C:\Program Files\Java\jdk1.7.0_01\jre\lib\ext\zipfs.jar;C:\Users\jim\work\RelaxFactory\rxf-server\target\rxf-server-0.8.0\WEB-INF\classes;C:\Users\jim\work\RelaxFactory\rxf-shared\target\classes;C:\Users\jim\.m2\repository\org\json\json\20090211\json-20090211.jar;C:\Users\jim\.m2\repository\javax\validation\validation-api\1.0.0.GA\validation-api-1.0.0.GA.jar;C:\Users\jim\.m2\repository\org\hibernate\hibernate-validator\4.2.0.Final\hibernate-validator-4.2.0.Final.jar;C:\Users\jim\.m2\repository\org\slf4j\slf4j-api\1.6.1\slf4j-api-1.6.1.jar;C:\Users\jim\.m2\repository\jetbrains\annotations\10.5.4\annotations-10.5.4.jar;C:\Users\jim\work\RelaxFactory\1xio\target\classes;C:\Users\jim\.m2\repository\com\google\code\gson\gson\2.1\gson-2.1.jar;C:\Users\jim\.m2\repository\com\google\web\bindery\requestfactory-server\2.5.0-rfmap-SNAPSHOT\requestfactory-server-2.5.0-rfmap-SNAPSHOT.jar;C:\Users\jim\.m2\repository\net\sf\jtidy\jtidy\r938\jtidy-r938.jar;C:\Users\jim\.m2\repository\org\apache\commons\commons-compress\1.4\commons-compress-1.4.jar;C:\Users\jim\.m2\repository\org\tukaani\xz\1.0\xz-1.0.jar;C:\Program Files (x86)\JetBrains\IntelliJ IDEA 10.5.2\lib\idea_rt.jar" com.intellij.rt.execution.application.AppMain rxf.server.CouchMetaDriver
//java 6 LOOPBACK detected
//opening /127.0.0.1:5984
//opening /127.0.0.1:5984
//opening /127.0.0.1:5984
//opening /127.0.0.1:5984
//opening /127.0.0.1:5984
//opening /127.0.0.1:5984
package rxf.server;
//generated

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

import static rxf.server.DbKeys.etype;

/**
 * generated drivers
 */
public interface CouchDriver {
    rxf.server.CouchTx createDb(java.lang.String db, java.lang.String validjson);

    public class createDbBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<rxf.server.CouchTx>[] dest;

        interface createDbTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
            CouchTx tx();

            void oneWay();
        }

        public class createDbActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
            public createDbActionBuilder(SynchronousQueue<rxf.server.CouchTx>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public createDbTerminalBuilder fire() {
                return new createDbTerminalBuilder() {
                    public CouchTx tx() {
                        try {
                            return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.createDb.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public void oneWay() {
                        BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    rxf.server.CouchMetaDriver.createDb.visit();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                };
            }

            @Override
            public createDbActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public createDbActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public createDbActionBuilder to(SynchronousQueue<rxf.server.CouchTx>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new createDbActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [db, validjson]");
        }

        static private final int parmsCount = 2;

        public createDbBuilder db(java.lang.String string) {
            parms.put(DbKeys.etype.db, string);
            return this;
        }

        public createDbBuilder validjson(java.lang.String string) {
            parms.put(DbKeys.etype.validjson, string);
            return this;
        }

    }

    rxf.server.CouchTx createDoc(java.lang.String db, java.lang.String docId, java.lang.String validjson);

    public class createDocBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<rxf.server.CouchTx>[] dest;

        interface createDocTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
            CouchTx tx();

            void oneWay();
        }

        public class createDocActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
            public createDocActionBuilder(SynchronousQueue<rxf.server.CouchTx>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public createDocTerminalBuilder fire() {
                return new createDocTerminalBuilder() {
                    public CouchTx tx() {
                        try {
                            return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.createDoc.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public void oneWay() {
                        BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    rxf.server.CouchMetaDriver.createDoc.visit();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                };
            }

            @Override
            public createDocActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public createDocActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public createDocActionBuilder to(SynchronousQueue<rxf.server.CouchTx>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new createDocActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [db, docId, validjson]");
        }

        static private final int parmsCount = 3;

        public createDocBuilder db(java.lang.String string) {
            parms.put(DbKeys.etype.db, string);
            return this;
        }

        public createDocBuilder docId(java.lang.String string) {
            parms.put(DbKeys.etype.docId, string);
            return this;
        }

        public createDocBuilder validjson(java.lang.String string) {
            parms.put(DbKeys.etype.validjson, string);
            return this;
        }

    }

    java.lang.String getDoc(java.lang.String db, java.lang.String docId);

    public class getDocBuilder<T> extends DbKeysBuilder<java.lang.String> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<java.lang.String>[] dest;

        interface getDocTerminalBuilder extends TerminalBuilder<java.lang.String> {
            java.lang.String pojo();

            Future<java.lang.String> future();
        }

        public class getDocActionBuilder extends ActionBuilder<java.lang.String> {
            public getDocActionBuilder(SynchronousQueue<java.lang.String>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public getDocTerminalBuilder fire() {
                return new getDocTerminalBuilder() {
                    public java.lang.String pojo() {
                        try {
                            return (java.lang.String) rxf.server.CouchMetaDriver.getDoc.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public Future<java.lang.String> future() {
                        try {

                            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<java.lang.String>() {
                                public java.lang.String call() throws Exception {
                                    return (java.lang.String) rxf.server.CouchMetaDriver.getDoc.visit();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
            }

            @Override
            public getDocActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public getDocActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public getDocActionBuilder to(SynchronousQueue<java.lang.String>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new getDocActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [db, docId]");
        }

        static private final int parmsCount = 2;

        public getDocBuilder db(java.lang.String string) {
            parms.put(DbKeys.etype.db, string);
            return this;
        }

        public getDocBuilder docId(java.lang.String string) {
            parms.put(DbKeys.etype.docId, string);
            return this;
        }

    }

    java.lang.String getRevision(java.lang.String db, java.lang.String docId);

    public class getRevisionBuilder<T> extends DbKeysBuilder<java.lang.String> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<java.lang.String>[] dest;

        interface getRevisionTerminalBuilder extends TerminalBuilder<java.lang.String> {
            CouchTx tx();

            Future<java.lang.String> future();
        }

        public class getRevisionActionBuilder extends ActionBuilder<java.lang.String> {
            public getRevisionActionBuilder(SynchronousQueue<java.lang.String>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public getRevisionTerminalBuilder fire() {
                return new getRevisionTerminalBuilder() {
                    public CouchTx tx() {
                        try {
                            return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.getRevision.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public Future<java.lang.String> future() {
                        try {

                            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<java.lang.String>() {
                                public java.lang.String call() throws Exception {
                                    return (java.lang.String) rxf.server.CouchMetaDriver.getRevision.visit();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
            }

            @Override
            public getRevisionActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public getRevisionActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public getRevisionActionBuilder to(SynchronousQueue<java.lang.String>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new getRevisionActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [db, docId]");
        }

        static private final int parmsCount = 2;

        public getRevisionBuilder db(java.lang.String string) {
            parms.put(DbKeys.etype.db, string);
            return this;
        }

        public getRevisionBuilder docId(java.lang.String string) {
            parms.put(DbKeys.etype.docId, string);
            return this;
        }

    }

    rxf.server.CouchTx updateDoc(java.lang.String db, java.lang.String docId, java.lang.String rev, java.lang.String validjson);

    public class updateDocBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<rxf.server.CouchTx>[] dest;

        interface updateDocTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
            CouchTx tx();

            void oneWay();

            Future<rxf.server.CouchTx> future();
        }

        public class updateDocActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
            public updateDocActionBuilder(SynchronousQueue<rxf.server.CouchTx>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public updateDocTerminalBuilder fire() {
                return new updateDocTerminalBuilder() {
                    public CouchTx tx() {
                        try {
                            return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.updateDoc.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public void oneWay() {
                        BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    rxf.server.CouchMetaDriver.updateDoc.visit();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    public Future<rxf.server.CouchTx> future() {
                        try {

                            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchTx>() {
                                public rxf.server.CouchTx call() throws Exception {
                                    return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.updateDoc.visit();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
            }

            @Override
            public updateDocActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public updateDocActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public updateDocActionBuilder to(SynchronousQueue<rxf.server.CouchTx>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new updateDocActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [db, docId, rev, validjson]");
        }

        static private final int parmsCount = 4;

        public updateDocBuilder db(java.lang.String string) {
            parms.put(DbKeys.etype.db, string);
            return this;
        }

        public updateDocBuilder docId(java.lang.String string) {
            parms.put(DbKeys.etype.docId, string);
            return this;
        }

        public updateDocBuilder rev(java.lang.String string) {
            parms.put(DbKeys.etype.rev, string);
            return this;
        }

        public updateDocBuilder validjson(java.lang.String string) {
            parms.put(DbKeys.etype.validjson, string);
            return this;
        }

    }

    rxf.server.CouchTx createNewDesignDoc(java.lang.String db, java.lang.String designDocId, java.lang.String validjson);

    public class createNewDesignDocBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<rxf.server.CouchTx>[] dest;

        interface createNewDesignDocTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
            CouchTx tx();

            void oneWay();
        }

        public class createNewDesignDocActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
            public createNewDesignDocActionBuilder(SynchronousQueue<rxf.server.CouchTx>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public createNewDesignDocTerminalBuilder fire() {
                return new createNewDesignDocTerminalBuilder() {
                    public CouchTx tx() {
                        try {
                            return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.createNewDesignDoc.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public void oneWay() {
                        BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    rxf.server.CouchMetaDriver.createNewDesignDoc.visit();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                };
            }

            @Override
            public createNewDesignDocActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public createNewDesignDocActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public createNewDesignDocActionBuilder to(SynchronousQueue<rxf.server.CouchTx>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new createNewDesignDocActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [db, designDocId, validjson]");
        }

        static private final int parmsCount = 3;

        public createNewDesignDocBuilder db(java.lang.String string) {
            parms.put(DbKeys.etype.db, string);
            return this;
        }

        public createNewDesignDocBuilder designDocId(java.lang.String string) {
            parms.put(DbKeys.etype.designDocId, string);
            return this;
        }

        public createNewDesignDocBuilder validjson(java.lang.String string) {
            parms.put(DbKeys.etype.validjson, string);
            return this;
        }

    }

    java.lang.String getDesignDoc(java.lang.String db, java.lang.String designDocId);

    public class getDesignDocBuilder<T> extends DbKeysBuilder<java.lang.String> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<java.lang.String>[] dest;

        interface getDesignDocTerminalBuilder extends TerminalBuilder<java.lang.String> {
            CouchTx tx();
        }

        public class getDesignDocActionBuilder extends ActionBuilder<java.lang.String> {
            public getDesignDocActionBuilder(SynchronousQueue<java.lang.String>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public getDesignDocTerminalBuilder fire() {
                return new getDesignDocTerminalBuilder() {
                    public CouchTx tx() {
                        try {
                            return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.getDesignDoc.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
            }

            @Override
            public getDesignDocActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public getDesignDocActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public getDesignDocActionBuilder to(SynchronousQueue<java.lang.String>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new getDesignDocActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [db, designDocId]");
        }

        static private final int parmsCount = 2;

        public getDesignDocBuilder db(java.lang.String string) {
            parms.put(DbKeys.etype.db, string);
            return this;
        }

        public getDesignDocBuilder designDocId(java.lang.String string) {
            parms.put(DbKeys.etype.designDocId, string);
            return this;
        }

    }

    rxf.server.CouchTx updateDesignDoc(java.lang.String db, java.lang.String designDocId, java.lang.String rev, java.lang.String validjson);

    public class updateDesignDocBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<rxf.server.CouchTx>[] dest;

        interface updateDesignDocTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
            CouchTx tx();

            void oneWay();
        }

        public class updateDesignDocActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
            public updateDesignDocActionBuilder(SynchronousQueue<rxf.server.CouchTx>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public updateDesignDocTerminalBuilder fire() {
                return new updateDesignDocTerminalBuilder() {
                    public CouchTx tx() {
                        try {
                            return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.updateDesignDoc.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public void oneWay() {
                        BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    rxf.server.CouchMetaDriver.updateDesignDoc.visit();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                };
            }

            @Override
            public updateDesignDocActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public updateDesignDocActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public updateDesignDocActionBuilder to(SynchronousQueue<rxf.server.CouchTx>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new updateDesignDocActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [db, designDocId, rev, validjson]");
        }

        static private final int parmsCount = 4;

        public updateDesignDocBuilder db(java.lang.String string) {
            parms.put(DbKeys.etype.db, string);
            return this;
        }

        public updateDesignDocBuilder designDocId(java.lang.String string) {
            parms.put(DbKeys.etype.designDocId, string);
            return this;
        }

        public updateDesignDocBuilder rev(java.lang.String string) {
            parms.put(DbKeys.etype.rev, string);
            return this;
        }

        public updateDesignDocBuilder validjson(java.lang.String string) {
            parms.put(DbKeys.etype.validjson, string);
            return this;
        }

    }

    rxf.server.CouchResultSet getView(java.lang.String db, java.lang.String view);

    public class getViewBuilder<T> extends DbKeysBuilder<rxf.server.CouchResultSet> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<rxf.server.CouchResultSet>[] dest;

        interface getViewTerminalBuilder extends TerminalBuilder<rxf.server.CouchResultSet> {
            CouchResultSet<rxf.server.CouchResultSet> rows();

            Future<rxf.server.CouchResultSet> future();

            void continuousFeed();

        }

        public class getViewActionBuilder extends ActionBuilder<rxf.server.CouchResultSet> {
            public getViewActionBuilder(SynchronousQueue<rxf.server.CouchResultSet>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public getViewTerminalBuilder fire() {
                return new getViewTerminalBuilder() {
                    public CouchResultSet<rxf.server.CouchResultSet> rows() {
                        try {
                            return (CouchResultSet<rxf.server.CouchResultSet>) rxf.server.CouchMetaDriver.getView.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public Future<rxf.server.CouchResultSet> future() {
                        try {

                            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchResultSet>() {
                                public rxf.server.CouchResultSet call() throws Exception {
                                    return (rxf.server.CouchResultSet) rxf.server.CouchMetaDriver.getView.visit();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public void continuousFeed() {
                        throw new AbstractMethodError();
                    }
                };
            }

            @Override
            public getViewActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public getViewActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public getViewActionBuilder to(SynchronousQueue<rxf.server.CouchResultSet>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new getViewActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [db, view]");
        }

        static private final int parmsCount = 2;

        public getViewBuilder db(java.lang.String string) {
            parms.put(DbKeys.etype.db, string);
            return this;
        }

        public getViewBuilder view(java.lang.String string) {
            parms.put(DbKeys.etype.view, string);
            return this;
        }

    }

    rxf.server.CouchTx sendJson(java.lang.String opaque, java.lang.String validjson);

    public class sendJsonBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<rxf.server.CouchTx>[] dest;

        interface sendJsonTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
            CouchTx tx();

            void oneWay();

            CouchResultSet<rxf.server.CouchTx> rows();

            Future<rxf.server.CouchTx> future();

            void continuousFeed();

        }

        public class sendJsonActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
            public sendJsonActionBuilder(SynchronousQueue<rxf.server.CouchTx>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public sendJsonTerminalBuilder fire() {
                return new sendJsonTerminalBuilder() {
                    public CouchTx tx() {
                        try {
                            return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.sendJson.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public void oneWay() {
                        BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    rxf.server.CouchMetaDriver.sendJson.visit();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    public CouchResultSet<rxf.server.CouchTx> rows() {
                        try {
                            return (CouchResultSet<rxf.server.CouchTx>) rxf.server.CouchMetaDriver.sendJson.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public Future<rxf.server.CouchTx> future() {
                        try {

                            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchTx>() {
                                public rxf.server.CouchTx call() throws Exception {
                                    return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.sendJson.visit();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public void continuousFeed() {
                        throw new AbstractMethodError();
                    }
                };
            }

            @Override
            public sendJsonActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public sendJsonActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public sendJsonActionBuilder to(SynchronousQueue<rxf.server.CouchTx>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new sendJsonActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [opaque, validjson]");
        }

        static private final int parmsCount = 2;

        public sendJsonBuilder opaque(java.lang.String string) {
            parms.put(DbKeys.etype.opaque, string);
            return this;
        }

        public sendJsonBuilder validjson(java.lang.String string) {
            parms.put(DbKeys.etype.validjson, string);
            return this;
        }

    }

    rxf.server.Rfc822HeaderState sendBlob(java.lang.String opaque, one.xio.MimeType mimetype, java.nio.ByteBuffer blob);

    public class sendBlobBuilder<T> extends DbKeysBuilder<rxf.server.Rfc822HeaderState> {
        Rfc822HeaderState rfc822HeaderState;
        java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<rxf.server.Rfc822HeaderState>[] dest;

        interface sendBlobTerminalBuilder extends TerminalBuilder<rxf.server.Rfc822HeaderState> {
            CouchTx tx();

            Future<rxf.server.Rfc822HeaderState> future();

            void oneWay();
        }

        public class sendBlobActionBuilder extends ActionBuilder<rxf.server.Rfc822HeaderState> {
            public sendBlobActionBuilder(SynchronousQueue<rxf.server.Rfc822HeaderState>... synchronousQueues) {
                super(synchronousQueues);
            }

            @Override
            public sendBlobTerminalBuilder fire() {
                return new sendBlobTerminalBuilder() {
                    public CouchTx tx() {
                        try {
                            return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.sendBlob.visit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public Future<rxf.server.Rfc822HeaderState> future() {
                        try {

                            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.Rfc822HeaderState>() {
                                public rxf.server.Rfc822HeaderState call() throws Exception {
                                    return (rxf.server.Rfc822HeaderState) rxf.server.CouchMetaDriver.sendBlob.visit();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    public void oneWay() {
                        BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    rxf.server.CouchMetaDriver.sendBlob.visit();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                };
            }

            @Override
            public sendBlobActionBuilder state(Rfc822HeaderState state) {
                return super.state(state);
            }

            @Override
            public sendBlobActionBuilder key(java.nio.channels.SelectionKey key) {
                return super.key(key);
            }
        }

        @Override
        public sendBlobActionBuilder to(SynchronousQueue<rxf.server.Rfc822HeaderState>... dest) {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new sendBlobActionBuilder(dest);

            throw new IllegalArgumentException("required parameters are: [opaque, mimetype, blob]");
        }

        static private final int parmsCount = 3;

        public sendBlobBuilder opaque(java.lang.String string) {
            parms.put(DbKeys.etype.opaque, string);
            return this;
        }

        public sendBlobBuilder mimetype(one.xio.MimeType mimetype) {
            parms.put(DbKeys.etype.mimetype, mimetype);
            return this;
        }

        public sendBlobBuilder blob(java.nio.ByteBuffer bytebuffer) {
            parms.put(DbKeys.etype.blob, bytebuffer);
            return this;
        }

    }
}
