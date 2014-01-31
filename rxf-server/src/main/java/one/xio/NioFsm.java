package one.xio;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NioFsm implements Closeable {
  private  final Queue<Object[]> q = new ConcurrentLinkedQueue<Object[]>();
  public  Thread selectorThread;
  public  boolean killswitch;
  private  Selector selector;

  public  Selector getSelector() {
    return selector;
  }

  public  void setSelector(Selector selector) {
    selector = selector;
  }

  /**
   * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
   *
   * @param channel the socketchanel
   * @param op      int ChannelSelector.operator
   * @param s       the payload: grammar {enum,data1,data..n}
   */
  public  void enqueue(SelectableChannel channel, int op, Object... s) {
    assert channel != null && !killswitch : "Server appears to have shut down, cannot enqueue";
    if (Thread.currentThread() == selectorThread)
      try {
        channel.register(getSelector(), op, s);
      } catch (ClosedChannelException e) {
        e.printStackTrace();
      }
    else {
      q.add(new Object[] {channel, op, s});
    }
    Selector selector1 = getSelector();
    if (null != selector1)
      selector1.wakeup();
  }

  public  String wheresWaldo(int... depth) {
    int d = depth.length > 0 ? depth[0] : 2;
    Throwable throwable = new Throwable();
    Throwable throwable1 = throwable.fillInStackTrace();
    StackTraceElement[] stackTrace = throwable1.getStackTrace();
    String ret = "";
    for (int i = 2, end = Math.min(stackTrace.length - 1, d); i <= end; i++) {
      StackTraceElement stackTraceElement = stackTrace[i];
      ret +=
          "\tat " + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName()
              + "(" + stackTraceElement.getFileName() + ":" + stackTraceElement.getLineNumber()
              + ")\n";

    }
    return ret;
  }

  public  void init(AsioVisitor protocoldecoder, String... a) throws IOException {

    setSelector(Selector.open());
    selectorThread = Thread.currentThread();

    synchronized (a) {
      long timeoutMax = 1024, timeout = 1;

      while (!killswitch) {
        while (!q.isEmpty()) {
          Object[] s = q.remove();
          SelectableChannel x = (SelectableChannel) s[0];
          Selector sel = getSelector();
          Integer op = (Integer) s[1];
          Object att = s[2];
          //          System.err.println("" + op + "/" + String.valueOf(att));
          try {
            x.configureBlocking(false);
            SelectionKey register = x.register(sel, op, att);
            assert null != register;
          } catch (Throwable e) {
            e.printStackTrace();
          }
        }
        int select = selector.select(timeout);

        timeout = 0 == select ? Math.min(timeout << 1, timeoutMax) : 1;
        if (0 != select)
          innerloop(protocoldecoder);
      }
    }
  }

  private  void innerloop(AsioVisitor protocoldecoder) throws IOException {
    Set<SelectionKey> keys = selector.selectedKeys();

    for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
      SelectionKey key = i.next();
      i.remove();

      if (key.isValid()) {
        SelectableChannel channel = key.channel();
        try {
          AsioVisitor m = inferAsioVisitor(protocoldecoder, key);

          if (key.isValid() && key.isWritable()) {
            if (!((SocketChannel) channel).socket().isOutputShutdown()) {
              m.onWrite(key);
            } else {
              key.cancel();
            }
          }
          if (key.isValid() && key.isReadable()) {
            if (!((SocketChannel) channel).socket().isInputShutdown()) {
              m.onRead(key);
            } else {
              key.cancel();
            }
          }
          if (key.isValid() && key.isAcceptable()) {
            m.onAccept(key);
          }
          if (key.isValid() && key.isConnectable()) {
            m.onConnect(key);
          }
        } catch (Throwable e) {
          Object attachment = key.attachment();
          if (attachment instanceof Object[]) {
            Object[] objects = (Object[]) attachment;
            System.err.println("BadHandler: " + java.util.Arrays.deepToString(objects));

          } else
            System.err.println("BadHandler: " + String.valueOf(attachment));

          if (AsioVisitor.$DBG) {
            AsioVisitor asioVisitor = inferAsioVisitor(protocoldecoder, key);
            if (asioVisitor instanceof AsioVisitor.Impl) {
              AsioVisitor.Impl visitor = (AsioVisitor.Impl) asioVisitor;
              if (AsioVisitor.$origins.containsKey(visitor)) {
                String s = AsioVisitor.$origins.get(visitor);
                System.err.println("origin" + s);
              }
            }
          }
          e.printStackTrace();
          key.attach(null);
          channel.close();
        }
      }
    }
  }

   AsioVisitor inferAsioVisitor(AsioVisitor default$, SelectionKey key) {
    Object attachment = key.attachment();
    AsioVisitor m;
    if (null == attachment)
      m = default$;
    if (attachment instanceof Object[]) {
      for (Object o : ((Object[]) attachment)) {
        attachment = o;
        break;
      }
    }
    if (attachment instanceof Iterable) {
      Iterable iterable = (Iterable) attachment;
      for (Object o : iterable) {
        attachment = o;
        break;
      }
    }
    m = attachment instanceof AsioVisitor ? (AsioVisitor) attachment : default$;
    return m;
  }

  public  void setKillswitch(boolean killswitch) {
    killswitch = killswitch;
  }

  @Override
public void close() throws IOException {
killswitch=true;selector.close();
}
}
