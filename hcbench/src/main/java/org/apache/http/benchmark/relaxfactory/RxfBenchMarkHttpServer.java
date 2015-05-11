package org.apache.http.benchmark.relaxfactory;

import com.databricks.fastbuffer.ByteBufferReader;
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
import java.util.concurrent.*;

import static bbcursive.Cursive.pre.*;
import static bbcursive.std.*;
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
 * Created by jim on 4/29/15.
 */
public class RxfBenchMarkHttpServer implements HttpServer {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();//(Math.max(2, WIDE)+3);
    public static final int WIDE = Runtime.getRuntime().availableProcessors();
    private ServerSocketChannel serverSocketChannel;


    private Impl protocoldecoder = new Impl() {
        public void onRead(SelectionKey key) throws Exception {

            Tx state  ;
            Object attachment = key.attachment();
            if (attachment instanceof Tx) {  //incomplete headers resuming
                state = Tx.acquireTx(key);
            } else {
                ByteBuffer byteBuffer;
                if (attachment instanceof ByteBuffer) {
                    byteBuffer = (ByteBuffer) attachment;
                } else byteBuffer = allocate(256);

                Tx tx = new Tx(key);
                (state = tx).hdr().headerBuf((ByteBuffer) byteBuffer.clear());
            }


            if (state.readHttpHeaders() && HttpMethod.GET == state.hdr().asRequest().httpMethod()) {
               ByteBuffer scratch = state.hdr().headerBuf();
                ByteBuffer bb = bb(scratch, rewind, toWs, slice, toWs, back1, flip);
                while (bb.hasRemaining() && bb.get() != '=') ;
//                String str = str(bb, noop);
                int count = (parseInt(bb));
                key.interestOps(0).selector().wakeup();

                ByteBuffer cursor ;
                if (scratch.limit()>= count) {
                    state.hdr().headerBuf(null);
                    cursor=scratch;
                }
                else cursor= allocateDirect(count);
                int r = Math.abs(key.hashCode());

                ByteBuffer tmp = ((ByteBuffer) cursor.rewind());
                int limit = cursor.limit();
                for (int i = 0; i < limit; i++) {
                    tmp.put(i,(byte) ((r + tmp.position()) % 96 + 32));
                }

                HttpResponse httpResponse = state.hdr().asResponse();
                httpResponse.status(HttpStatus.$200).headerStrings().clear();
                httpResponse.headerString(Content$2dType, text.contentType).headerString(Content$2dLength, str(count));

                finishWrite(key, key1 -> {
                    key.interestOps(OP_READ).selector().wakeup();
                    key.attach(cursor);
                }, cat(state.hdr().asResponse().asByteBuffer(), bb(cursor, rewind)));


            }
        }
    };
    private int regCount=0;

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
        ShardNode2[] shardNode2s = new ShardNode2[WIDE];
        for (int i = 0; i < shardNode2s.length; i++) {
            int finalI = i;
            ShardNode2 shardNode2 = new ShardNode2();
            shardNode2s[finalI]=shardNode2;
            executorService.execute(() -> {
                try {
                    shardNode2.init(protocoldecoder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        Thread.sleep(0);
        Thread.sleep(0);
        Thread.sleep(0);
        serverSocketChannel = (ServerSocketChannel) ServerSocketChannel.open().bind(new InetSocketAddress(/*InetAddress.getLoopbackAddress(),*/ Benchmark.PORT)/*, 2048*/).configureBlocking(false);
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);//.setOption(StandardSocketOptions.IP_TOS, 0x10 );
        boolean first = true;
      shardNode2s[regCount++%shardNode2s.length]. enqueue(serverSocketChannel, OP_ACCEPT, new Impl() {
            @Override
            public void onAccept(SelectionKey key) throws Exception {
                ServerSocketChannel c = (ServerSocketChannel) key.channel();
                SocketChannel newSocket = c.accept();
//        IPTOS_LOWCOST (0x02)
//        IPTOS_RELIABILITY (0x04)
//        IPTOS_THROUGHPUT (0x08)
//        IPTOS_LOWDELAY (0x10)
//                accept.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE).setOption(StandardSocketOptions.IP_TOS, 0x10);
                newSocket.configureBlocking(false);
                shardNode2s[regCount++%shardNode2s.length].enqueue(newSocket, OP_READ /*| OP_WRITE*/);

            }
        });


    }

    public void shutdown() {
        AsyncSingletonServer.killswitch.set(true);
        executorService.shutdownNow();
        try {
            serverSocketChannel.close();
        } catch (Throwable e) {

        }
    }

    public static void main(String[] args) throws Exception {

        RxfBenchMarkHttpServer rxfBenchMarkHttpServer = new RxfBenchMarkHttpServer(Benchmark.PORT);
        rxfBenchMarkHttpServer.start();


        while (!AsyncSingletonServer.killswitch.get()) Thread.sleep(1000);
    }
}
