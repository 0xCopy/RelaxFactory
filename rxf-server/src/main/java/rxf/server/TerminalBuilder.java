package rxf.server;

public abstract class TerminalBuilder<T> {

    abstract void toVoid();

    abstract CouchResultSet<T> rs();

    abstract CouchTx tx();
}

class DefaultTerminalBuilder<T> extends TerminalBuilder<T> {

    @Override
    void toVoid() {

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