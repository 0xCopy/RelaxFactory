package rxf.server;

import java.util.EnumMap;
import java.util.concurrent.SynchronousQueue;

import static rxf.server.DbKeys.*;

public interface CouchDriver {
    CouchTx createDb(String db, String docId);


    public class createDbBuilder<T> extends DbKeysBuilder<CouchTx> {
        Rfc822HeaderState rfc822HeaderState;
        EnumMap<etype, Object> parms = new EnumMap<etype, Object>(etype.class);
        private SynchronousQueue<T>[] dest;


        public ActionBuilder<CouchTx> to() {
            this.dest = dest;
            if (parms.size() == parmsCount)
                return new ActionBuilder<CouchTx>() {
                    @Override
                    public TerminalBuilder<CouchTx> fire() {
                        return new AbstractTerminalBuilder();
                    }
                };
            throw new IllegalArgumentException("required paramters are: [db, docId]");
        }

        static private final int parmsCount = 2;

        public createDbBuilder db(String string) {
            parms.put(DbKeys.etype.db, string);
            return this;
        }

        public createDbBuilder docId(String string) {
            parms.put(DbKeys.etype.docId, string);
            return this;
        }

        @Override
        public ActionBuilder<CouchTx> to(SynchronousQueue<CouchTx>... clients) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

    }

}

