package rxf.server;

import java.util.concurrent.Future;

/**
 * Created by IntelliJ IDEA.
 * User: jim
 * Date: 5/29/12
 * Time: 3:27 AM
 */
public class AbstractTerminalBuilder<T> extends TerminalBuilder<T> {
  @Override
  void toVoid() {
    throw new AbstractMethodError();
  }

  @Override
  CouchResultSet<T> rs() {
    throw new AbstractMethodError();

  }

  @Override
  CouchTx tx() throws Exception {
    throw new AbstractMethodError();
  }

  @Override
  public Future<T> future() {
    throw new AbstractMethodError();
  }
}
