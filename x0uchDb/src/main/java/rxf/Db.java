package rxf;

import one.xio.HttpMethod;
import rxf.server.PreRead;
import rxf.server.RelaxFactoryServer;
import rxf.server.Rfc822HeaderState;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.channels.SelectionKey.OP_READ;
import static one.xio.AsioVisitor.Impl;
import static rxf.server.BlobAntiPatternRelic.deepToString;
import static rxf.server.CouchNamespace.NAMESPACE;
import static rxf.server.RelaxFactoryServer.UTF8;
import static rxf.server.Rfc822HeaderState.HttpRequest;

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 1/7/13
 * Time: 2:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class Db {
    public Db() throws IOException {
        RelaxFactoryServer.App.get().launchVhost("127.0.0.1", 5984, new Impl() {
            public void onAccept(SelectionKey key) throws IOException {
                ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                SocketChannel accept = channel.accept();
                accept.configureBlocking(false);
                RelaxFactoryServer.App.get().enqueue(accept, OP_READ, new Impl() {
                    public void onRead(SelectionKey key) throws Exception {
                        SocketChannel channel = (SocketChannel) key.channel();

                        ByteBuffer cursor = ByteBuffer.allocateDirect(channel.socket().getReceiveBufferSize());
                        int read = channel.read(cursor);
                        if (-1 != read) {

                            HttpMethod method = null;
                            HttpRequest httpRequest = null;
                            try {
                                //find the method to dispatch
                                Rfc822HeaderState state = new Rfc822HeaderState().apply((ByteBuffer) cursor.flip());
                                httpRequest = state.$req();
                                if (RelaxFactoryServer.App.get().isDEBUG_SENDJSON()) {
                                    System.err.println(deepToString(UTF8.decode((ByteBuffer) httpRequest.headerBuf().duplicate().rewind())));
                                }
                                String method1 = httpRequest.method();
                                method = HttpMethod.valueOf(method1);

                            } catch (Exception e) {
                            }

                            if (null != method) {

                                Set<Map.Entry<Pattern, Class<? extends Impl>>> entries = NAMESPACE.get(method).entrySet();
                                String path = httpRequest.path();
                                for (Map.Entry<Pattern, Class<? extends Impl>> visitorEntry : entries) {
                                    Matcher matcher = visitorEntry.getKey().matcher(path);
                                    if (matcher.find()) {
                                        if (RelaxFactoryServer.App.get().isDEBUG_SENDJSON()) {
                                            System.err.println("+?+?+? using " + matcher.toString());
                                        }
                                        Class<? extends Impl> value = visitorEntry.getValue();

                                        Impl impl = value.newInstance();
                                        Object a[] = {impl, httpRequest, cursor};
                                        key.attach(a);
                                        if (PreRead.class.isAssignableFrom(value)) {
                                            impl.onRead(key);
                                        }

                                        key.selector().wakeup();

                                        return;
                                    }

                                }
                                System.err.println(deepToString("!!!1!1!!", "404", path, "using", NAMESPACE));
                            } else {
                                ((SocketChannel) key.channel()).socket().close();//cancel();

                            }
                        } else {
                            ((SocketChannel) key.channel()).socket().close();//cancel();
                        }
                    }
                });

            }

        });
    }

    public static void main(String... args) throws IOException, InterruptedException {
        Db db = new Db();
        synchronized (args) {
            args.wait();
        }
    }
}
