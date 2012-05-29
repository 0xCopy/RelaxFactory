package rxf.server;

import java.util.concurrent.Future;

public abstract class TerminalBuilder<T> {

  abstract void toVoid() throws Exception;

  abstract CouchResultSet<T> rs() throws Exception;

  abstract CouchTx tx() throws Exception;

  abstract public Future<T> future() throws Exception;
}
