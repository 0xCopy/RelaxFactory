package rxf.server;

public abstract class TaskBuilder<T> {

    abstract TerminalBuilder<T> now();

    abstract TerminalBuilder<T> future();


}
