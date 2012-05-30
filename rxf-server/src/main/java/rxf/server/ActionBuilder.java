package rxf.server;

import java.nio.channels.SelectionKey;
import java.util.concurrent.SynchronousQueue;

/**
 * User: jim
 * Date: 5/29/12
 * Time: 1:58 PM
 */
public abstract class ActionBuilder<T> {

    Rfc822HeaderState state;
    SelectionKey key;
    static ThreadLocal<ActionBuilder> currentAction = new ThreadLocal<ActionBuilder>();
    private SynchronousQueue<T>[] synchronousQueues;

    public ActionBuilder(SynchronousQueue<T>... synchronousQueues) {
        this.synchronousQueues = synchronousQueues;
        currentAction.set(this);
    }

    public SynchronousQueue<T>[] sync() {
        return this.synchronousQueues;
    }

    public Rfc822HeaderState state() {
        return this.state;
    }

    public SelectionKey key() {
        return this.key;
    }

    abstract protected <B extends TerminalBuilder<T>> B fire() throws Exception;


    protected <B extends ActionBuilder<T>> B state(Rfc822HeaderState state) {
        this.state = state;
        return (B) this;
    }


    protected <B extends ActionBuilder<T>> B key(SelectionKey key) {
        this.key = key;

        return (B) this;
    }

}
