package org.apache.http.benchmark.relaxfactory;

import one.xio.AsioVisitor;
import one.xio.AsioVisitor.Impl;
import one.xio.AsyncSingletonServer;
 import one.xio.HttpMethod;
import one.xio.HttpStatus;
import org.apache.http.benchmark.Benchmark;
import org.apache.http.benchmark.HttpServer;
import rxf.core.Rfc822HeaderState.HttpResponse;
import rxf.core.Tx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

import static bbcursive.Cursive.pre.*;
import static bbcursive.std.bb;
import static bbcursive.std.str;
import static java.lang.StrictMath.min;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;
import static one.xio.AsioVisitor.Helper.finishWrite;
import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.Content$2dType;
import static one.xio.MimeType.text;

/**
 * Created by jim per 5/19/14.
 */
class ShardServer2 implements AsyncSingletonServer {

    private static Thread selectorThread;
    private static Selector selector;

    /**
     * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
     *
     * @param channel the socketchanel
     * @param op int ChannelSelector.operator
     * @param s the payload: grammar {enum,data1,data..n}
     */
    public static void enqueue(SelectableChannel channel, int op, Object... s) {
        assert channel != null && !killswitch.get() : "Server appears to have shut down, cannot enqueue";
        if (Thread.currentThread() == selectorThread)
            try {
                channel.register(AsioVisitor.Helper.getSelector(), op, s);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        else {
            q.add(new Object[] {channel, op, s});
        }
    }

    public static void init(AsioVisitor protocoldecoder) throws IOException {

       selector=(Selector.open());
        selectorThread = Thread.currentThread();

        long timeoutMax = 1024, timeout = 1;
      /*synchronized (killswitch)*/{
            while (!killswitch.get()) {
                while (!q.isEmpty()) {
                    Object[] s = q.poll();
                    if (null != s) {
                        SelectableChannel x = (SelectableChannel) s[0];

                        Integer op = (Integer) s[1];
                        Object att = s[2];

                        try {
                            x.configureBlocking(false);
                            SelectionKey register = x.register(selector, op, att);
                            assert null != register;
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
                int select = selector.select(timeout);

                timeout = 0 == select ? min(timeout << 1, timeoutMax) : 1;
                if (0 != select)
                    innerloop(protocoldecoder);
            }
        }}

    public static void innerloop(AsioVisitor protocoldecoder) throws IOException {
        Set<SelectionKey> keys = selector.selectedKeys();

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
                    if (!(attachment instanceof Object[])) {
                        System.err.println("BadHandler: " + String.valueOf(attachment));
                    } else {
                        Object[] objects = (Object[]) attachment;
                        System.err.println("BadHandler: " + java.util.Arrays.deepToString(objects));
                    }
                    if (AsioVisitor.$DBG) {
                        AsioVisitor asioVisitor = inferAsioVisitor(protocoldecoder, key);
                        if (asioVisitor instanceof Impl) {
                            Impl visitor = (Impl) asioVisitor;
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
}
/**
 * Created by jim on 4/29/15.
 */
public class RxfBenchMarkHttpServer implements HttpServer {


    final private static ExecutorService executorService = Executors.newCachedThreadPool();//(Math.max(2, WIDE)+3);
    public static final int WIDE = Runtime.getRuntime().availableProcessors();
    private ServerSocketChannel serverSocketChannel;

    public RxfBenchMarkHttpServer(int port) {

    }

    public String getName() {
        return "RelaxFactory";
    }

    public String getVersion() {
        return "almost.1";
    }

    public int getPort() {
        return Benchmark.PORT;
    }

    public void start() throws Exception {
        AsyncSingletonServer.killswitch.set(false);

        Impl protocoldecoder = new Impl() {
            public void onRead(SelectionKey key) throws Exception {

                Tx state  /*.payload(allocateDirect(444))*/;
                Object attachment = key.attachment();
                if (attachment instanceof Tx) {  //incomplete headers resuming
                    state = Tx.acquireTx(key);
                } else {
                    ByteBuffer byteBuffer;
                    if (attachment instanceof ByteBuffer) {
                        byteBuffer = (ByteBuffer) attachment;
                    } else byteBuffer = allocate(256);

                    final Tx tx = new Tx(key);
                    (state = tx).hdr().headerBuf((ByteBuffer) byteBuffer.clear());
                }


                if (state.readHttpHeaders() && HttpMethod.GET == state.hdr().asRequest().httpMethod()) {
                   final  ByteBuffer scratch = state.hdr().headerBuf();
                    ByteBuffer bb = bb(scratch, rewind, toWs, slice, toWs, back1, flip);
                    while (bb.hasRemaining() && bb.get() != '=') ;
                    String str = str(bb, noop);
                    int count = (Integer.parseInt(str));
                    key.interestOps(0).selector().wakeup();

                    ByteBuffer cursor ;
                    if (scratch.limit() == count) {
                        state.hdr().headerBuf(null);cursor=scratch;
                    }
                    else cursor= allocateDirect(count);
                    int r = Math.abs(key.hashCode());

//                    int deep = count / WIDE;
/*
                    for (int i = 0; i < WIDE; i++) {
                        ByteBuffer tmp = ((ByteBuffer) cursor.position(i * deep)).slice();
                        final int finalI = i;


                        ByteBuffer b = tmp;

                        int j = 0;
                        try {
                            for (j = 0; j < deep; j++) tmp.put(  (byte) ((r + j+deep* finalI) % 96 + 32));
                        } catch (Exception e) {
                            int x = j;
                            e.printStackTrace();
                        }
                        ;

                    }
                    int leftover = count % deep;*/
                    int finalCount = count;
                    ByteBuffer tmp = (ByteBuffer) cursor.rewind();
                    while (tmp.hasRemaining()) tmp.put((byte) ((r + tmp.position()) % 96 + 32));

                    HttpResponse httpResponse = state.hdr().asResponse();
                    httpResponse.status(HttpStatus.$200).headerStrings().clear();
                    httpResponse.headerString(Content$2dType, text.contentType).headerString(Content$2dLength, str(count));
                    finishWrite(key, key1 -> {
                        key.interestOps(OP_READ).selector().wakeup();
                        key.attach(cursor);
                    }, state.hdr().asResponse().asByteBuffer(), bb(cursor, rewind));


                }
            }
        };

        serverSocketChannel = (ServerSocketChannel) ServerSocketChannel.open().bind(new InetSocketAddress(/*InetAddress.getLoopbackAddress(),*/ Benchmark.PORT), 2048).configureBlocking(false);
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);//.setOption(StandardSocketOptions.IP_TOS, 0x10 );

        ShardServer2.enqueue(serverSocketChannel, OP_ACCEPT, new Impl() {
            @Override
            public void onAccept(SelectionKey key) throws Exception {
                ServerSocketChannel c = (ServerSocketChannel) key.channel();
                SocketChannel accept = c.accept();
//        IPTOS_LOWCOST (0x02)
//        IPTOS_RELIABILITY (0x04)
//        IPTOS_THROUGHPUT (0x08)
//        IPTOS_LOWDELAY (0x10)
                accept.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE).setOption(StandardSocketOptions.IP_TOS, 0x10);
                accept.configureBlocking(false);
                accept.register(key.selector(), OP_READ /*| OP_WRITE*/, protocoldecoder);

            }
        });

        executorService.submit(() -> {
            try {
                ShardServer2.init(protocoldecoder);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    public void shutdown() {
        AsyncSingletonServer.killswitch.set(true);
        executorService.shutdown();
        try {
            serverSocketChannel.close();
        } catch (IOException e) {

        }
    }

    public static void main(String[] args) throws Exception {

        RxfBenchMarkHttpServer rxfBenchMarkHttpServer = new RxfBenchMarkHttpServer(Benchmark.PORT);
        rxfBenchMarkHttpServer.start();


        while (!AsyncSingletonServer.killswitch.get()) Thread.sleep(1000);
    }
}
