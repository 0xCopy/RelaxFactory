package rxf.server;

/**
 * Created by IntelliJ IDEA.
 * User: jim
 * Date: 5/29/12
 * Time: 3:27 AM
 * To change this template use File | Settings | File Templates.
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
    CouchTx tx() {
        throw new AbstractMethodError();
    }
}
