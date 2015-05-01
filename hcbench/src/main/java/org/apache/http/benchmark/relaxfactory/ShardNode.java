package org.apache.http.benchmark.relaxfactory;

import one.xio.AsioVisitor;
import one.xio.AsyncSingletonServer;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

import static java.lang.StrictMath.min;
import static one.xio.AsioVisitor.Helper.bye;

/**
 * http://askubuntu.com/a/162230
 */
public class ShardNode implements AsyncSingletonServer {

    //    Queue<Object[]> q = new ConcurrentLinkedQueue<>();
    public Thread selectorThread;
    public Selector selector;{
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
     *
     * @param channel the socketchanel
     * @param op      int ChannelSelector.operator
     * @param tuple       the payload: grammar {enum,data1,data..n}
     */
    static   public void enqueue(SelectableChannel channel, int op, Object... tuple) throws IOException {
        assert channel != null && !killswitch.get() : "Server appears to have shut down, cannot enqueue";
        q.add(new Object[]{channel, op, tuple});
    }

    public void spin(AsioVisitor protocoldecoder) throws IOException {


//        selectorThread = Thread.currentThread();

        long timeoutMax = 1024;
        long timeout = 1;

        while (!killswitch.get()) {
            do {
                Object[] inbound ;
                if (null != (inbound= q.poll())) {
                    SelectableChannel x = (SelectableChannel) inbound[0];
                    Integer op = (Integer) inbound[1];
                    Object att = inbound[2];

                    try {
                        x.configureBlocking(false);
                        SelectionKey register = x.register(selector, op, att);
                        assert null != register;
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            } while (!q.isEmpty());
            int select = selector.select(timeout);

            timeout = 0 == select ? min(timeout << 1, timeoutMax) : 1;
            if (0 != select)
                innerloop(protocoldecoder);

        }
    }


    public void innerloop(AsioVisitor protocoldecoder) throws IOException {
        Set<SelectionKey> keys = selector.selectedKeys();

        for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext(); ) {
            SelectionKey key = i.next();

            if (key.isValid()) {
                SelectableChannel channel = key.channel();
                try {
                    AsioVisitor m = inferAsioVisitor(protocoldecoder, key);

                    if (key.isValid() && key.isAcceptable()) {
                        m.onAccept(key);
                    } else {
                        Socket socket = ((SocketChannel) channel).socket();
                        if (key.isValid() && key.isWritable()) if (!socket.isOutputShutdown()) m.onWrite(key);
                        else bye(key);
                        if (key.isValid() && key.isReadable()) if (!socket.isInputShutdown())  m.onRead(key);
                        else bye(key);
                    }
                    i.remove();
                } catch (Throwable e) {
                    Object attachment = key.attachment();
                    if (!(attachment instanceof Object[])) {
                        System.err.println("BadHandler: " + String.valueOf(attachment));
                    } else {
                        Object[] objects = (Object[]) attachment;
                        System.err.println("BadHandler: " + java.util.Arrays.deepToString(objects));
                    }
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

    static public AsioVisitor inferAsioVisitor(AsioVisitor default$, SelectionKey key) {
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
}
