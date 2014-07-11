package rxf.core;

import one.xio.AsioVisitor;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.StrictMath.min;
import static one.xio.AsioVisitor.FSM;

/**
 * Created by jim on 5/19/14.
 */
public class Server {
  public static final Queue<Object[]> q = new ConcurrentLinkedQueue<>();
  public static final boolean DEBUG_SENDJSON = Config.get("RXF_DEBUG_SENDJSON", "false").equals(
      "true");
  public static boolean killswitch;

  /**
   * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
   * 
   * @param channel the socketchanel
   * @param op int ChannelSelector.operator
   * @param s the payload: grammar {enum,data1,data..n}
   */
  public static void enqueue(SelectableChannel channel, int op, Object... s) {
    assert channel != null && !killswitch : "Server appears to have shut down, cannot enqueue";

    if (Thread.currentThread() == FSM.selectorThread)
      try {
        channel.register(FSM.getSelector(), op, s);
      } catch (ClosedChannelException e) {
        e.printStackTrace();
      }
    else {
      q.add(new Object[] {channel, op, s});
    }
    Selector selector1 = FSM.getSelector();
    if (null != selector1)
      selector1.wakeup();
  }

  public static void init(AsioVisitor protocoldecoder) throws IOException {

    FSM.setSelector(Selector.open());

    FSM.selectorThread = Thread.currentThread();

    long timeoutMax = 1024, timeout = 1;

    while (!killswitch) {
      while (!q.isEmpty()) {
        Object[] s = q.remove();
        SelectableChannel x = (SelectableChannel) s[0];
        Selector sel = FSM.getSelector();
        Integer op = (Integer) s[1];
        Object att = s[2];

        try {
          x.configureBlocking(false);
          SelectionKey register = x.register(sel, op, att);
          assert null != register;
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
      int select = FSM.getSelector().select(timeout);

      timeout = 0 == select ? min(timeout << 1, timeoutMax) : 1;
      if (0 != select)
        innerloop(protocoldecoder);
    }
  }

  public static void innerloop(AsioVisitor protocoldecoder) throws IOException {
    Set<SelectionKey> keys = FSM.getSelector().selectedKeys();

    for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
      SelectionKey key = i.next();
      i.remove();

      if (key.isValid()) {
        SelectableChannel channel = key.channel();
        try {
          AsioVisitor m = inferAsioVisitor(protocoldecoder, key);

          if (key.isValid() && key.isWritable()) {
            if (((SocketChannel) channel).socket().isOutputShutdown()) {
              key.cancel();
            } else {
              m.onWrite(key);
            }
          }
          if (key.isValid() && key.isReadable()) {
            if (((SocketChannel) channel).socket().isInputShutdown()) {
              key.cancel();
            } else {
              m.onRead(key);
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

  public static AsioVisitor inferAsioVisitor(AsioVisitor default$, SelectionKey key) {
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

  public static void setKillswitch(boolean killswitch) {
    Server.killswitch = killswitch;
  }

  public static Selector getSelector() {
    return FSM.getSelector();
  }
}
