package rxf.server;

import one.xio.AsioVisitor;
import one.xio.HttpStatus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;

public interface RelaxFactoryServer {

    Charset UTF8 = Charset.forName("UTF8");
    InheritableThreadLocal<RelaxFactoryServer> rxfTl = new InheritableThreadLocal<>();

    Selector getSelector();

    RelaxFactoryServer setSelector(Selector selector);

    Object[] toArray(Object... t);

    RelaxFactoryServer enqueue(SelectableChannel channel, int op, Object... s)
            throws ClosedChannelException;

    RelaxFactoryServer response(SelectionKey key, HttpStatus httpStatus)
            throws IOException;

    RelaxFactoryServer init(AsioVisitor protocoldecoder, String... a)
            throws IOException;

    AsioVisitor inferAsioVisitor(AsioVisitor default$, SelectionKey key);

    Charset getUTF8();

    Thread getSelectorThread();

    RelaxFactoryServer setSelectorThread(Thread selectorThread);

    boolean isKillswitch();

    RelaxFactoryServer setKillswitch(boolean killswitch);

    ConcurrentLinkedQueue<Object[]> getQ();

    RelaxFactoryServer setQ(ConcurrentLinkedQueue<Object[]> q);

    boolean isDEBUG_SENDJSON();

    RelaxFactoryServer setDEBUG_SENDJSON(boolean DEBUG_SENDJSON);

    InetAddress getLOOPBACK();

    InetSocketAddress getCOUCHADDR();

    RelaxFactoryServer setCOUCHADDR(InetSocketAddress COUCHADDR);

    ScheduledExecutorService getEXECUTOR_SERVICE();

    RelaxFactoryServer setEXECUTOR_SERVICE(
            ScheduledExecutorService EXECUTOR_SERVICE);

    int getReceiveBufferSize();

    RelaxFactoryServer setReceiveBufferSize(int receiveBufferSize);

    int getSendBufferSize();

    RelaxFactoryServer setSendBufferSize(int sendBufferSize);

    RelaxFactoryServer launchVhost(String hostname, int port, AsioVisitor topLevel)
            throws UnknownHostException;
/*
    RelaxFactoryServer start() throws IOException;*/

    int getPort();

    RelaxFactoryServer setPort(int port);

    RelaxFactoryServer stop() throws IOException;

    AsioVisitor getTopLevel();

    RelaxFactoryServer setTopLevel(AsioVisitor topLevel);

    InetAddress getHostname();

    RelaxFactoryServer setHostname(InetAddress hostname);

    ServerSocketChannel getServerSocketChannel();

    RelaxFactoryServer setServerSocketChannel(ServerSocketChannel serverSocketChannel);

    class App {
        public static RelaxFactoryServer get() {
            RelaxFactoryServer relaxFactoryServer = rxfTl.get();
            if (null == relaxFactoryServer) {
                relaxFactoryServer = RelaxFactoryServerImpl.createRelaxFactoryServerImpl();
                rxfTl.set(relaxFactoryServer);
            }
            return relaxFactoryServer;
        }
    }
}
