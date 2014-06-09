package rxf.couch;

import rxf.core.Config;
import rxf.core.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.*;

public class CouchConnectionFactory {

    public static final int CONNECTION_POOL_SIZE = Integer.parseInt(Config.get("RXF_CONNECTION_POOL_SIZE", "20"));

    public static InetSocketAddress COUCHADDR;

    static {

        String rxfcouchprefix = Config.get("RXF_COUCH_PREFIX", "http://localhost:5984");
        try {
            URI uri = new URI(rxfcouchprefix);
            int port = uri.getPort();
            port = -1 != port ? port : 80;
            setCOUCHADDR(new InetSocketAddress(uri.getHost(), port));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private static Queue<SocketChannel> pool = new ArrayBlockingQueue<>(CONNECTION_POOL_SIZE);

    public static SocketChannel createCouchConnection() {
        SocketChannel ret = null;
        while (!Server.killswitch) {
            SocketChannel socketChannel = pool.poll();
            if (null != socketChannel && socketChannel.isConnected()) {
                ret = socketChannel;
                break;
            }
            try {
                SocketChannel channel = SocketChannel.open(getCOUCHADDR());
                channel.configureBlocking(false);
                ret = channel;
                break;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
            }
        }
        return ret;
    }

    public static void recycleChannel(SocketChannel channel) {

            if (channel.isConnected())
                if (!pool.offer(channel))
                    try {          channel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static InetSocketAddress getCOUCHADDR() {
        return COUCHADDR;
    }

    public static void setCOUCHADDR(InetSocketAddress COUCHADDR) {
        CouchConnectionFactory.COUCHADDR = COUCHADDR;
    }

}
