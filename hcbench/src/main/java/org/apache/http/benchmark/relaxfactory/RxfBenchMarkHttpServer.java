package org.apache.http.benchmark.relaxfactory;

import one.xio.AsioVisitor.Helper.F;
import one.xio.AsioVisitor.Impl;
import one.xio.AsyncSingletonServer;
import one.xio.AsyncSingletonServer.SingleThreadSingletonServer;
import one.xio.HttpMethod;
import one.xio.HttpStatus;
import org.apache.http.benchmark.Benchmark;
import org.apache.http.benchmark.HttpServer;
import org.jboss.netty.buffer.ChannelBuffers;
import rxf.core.Rfc822HeaderState.HttpRequest;
import rxf.core.Rfc822HeaderState.HttpResponse;
import rxf.core.Tx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static bbcursive.Cursive.pre.*;
import static bbcursive.std.bb;
import static bbcursive.std.cat;
import static bbcursive.std.str;
import static java.nio.ByteBuffer.wrap;
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


    final private static ExecutorService executorService=Executors.newCachedThreadPool();//(Math.max(2, WIDE)+3);
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
                //toRead begin
                ((F) ke -> {

                    Helper.toRead(key, new F() {

                        int count = 0;
                        final Tx state = new Tx(key)/*.payload(allocateDirect(444))*/;


                        @Override
                        public void apply(SelectionKey key) throws Exception {
                            Object root = key.attachment();
                            boolean b = state.readHttpHeaders();
                            HttpRequest httpRequest;
                            ;
                            if (b && HttpMethod.GET == (httpRequest = state.hdr().asRequest()).httpMethod()) {
                                ByteBuffer bb = bb(state.hdr().headerBuf(), rewind, toWs, slice, toWs, back1, flip);
                                while (bb.hasRemaining() && bb.get() != '=') ;
                                String str = str(bb, noop);
                                count = (Integer.parseInt(str));



                                byte[] buf = new byte[count];
                                int r = Math.abs(buf.hashCode());
                                for (int i = 0; i < count; i++) {
                                    buf[i] = (byte) ((r + i) % 96 + 32);
                                }
                                HttpResponse httpResponse = state.hdr().asResponse();
                                httpResponse.status(HttpStatus.$200).headerStrings().clear();
                                httpResponse.headerString(Content$2dType, text.contentType).headerString(Content$2dLength, str(count));

                                finishWrite(key, key1 -> {
                                    key1.interestOps(OP_READ).selector().wakeup();
                                    key1.attach(null);

                                },  (ByteBuffer)state.hdr().asResponse().asByteBuffer(),wrap(buf));
                            }
                        }
                    });
                }).apply(key);
                //toRead end
            }
        };

        serverSocketChannel = (ServerSocketChannel) ServerSocketChannel.open().bind(new InetSocketAddress(/*InetAddress.getLoopbackAddress(),*/ Benchmark.PORT),2048).configureBlocking(false);
//        IPTOS_LOWCOST (0x02)
//        IPTOS_RELIABILITY (0x04)
//        IPTOS_THROUGHPUT (0x08)
//        IPTOS_LOWDELAY (0x10)
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE);//.setOption(StandardSocketOptions.IP_TOS, 0x10 );

        SingleThreadSingletonServer.enqueue(serverSocketChannel, OP_ACCEPT, new Impl() {
            @Override
            public void onAccept(SelectionKey key) throws Exception {
                ServerSocketChannel c = (ServerSocketChannel) key.channel();
                SocketChannel accept = c.accept();
                accept.setOption(StandardSocketOptions.TCP_NODELAY,Boolean.TRUE).setOption(StandardSocketOptions.IP_TOS, 0x10)   ;
                accept.configureBlocking(false);
                accept.register(key.selector(), OP_READ /*| OP_WRITE*/,protocoldecoder);

            }
        });

         executorService.submit(() -> {
             try {
                 SingleThreadSingletonServer.init(protocoldecoder);
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


        while(!AsyncSingletonServer.killswitch.get())Thread.sleep(1000);
    }
}
