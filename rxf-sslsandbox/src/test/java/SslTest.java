import one.xio.AsioVisitor;
import one.xio.AsioVisitor.Helper;
import one.xio.AsioVisitor.Helper.F;
import one.xio.HttpMethod;
import one.xio.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rxf.core.Rfc822HeaderState;
import rxf.core.Server;
import rxf.core.Tx;
import rxf.rpc.RpcHelper;
import rxf.web.inf.ProtocolMethodDispatch;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.concurrent.Future;

import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.AsioVisitor.Helper.sslGoal;
import static one.xio.AsioVisitor.Helper.toRead;
import static org.junit.Assert.fail;
import static rxf.core.Server.enqueue;
import static rxf.core.Server.init;
import static rxf.core.Server.setKillswitch;
import static rxf.rpc.RpcHelper.getEXECUTOR_SERVICE;
import static rxf.rpc.RpcHelper.setDEBUG_SENDJSON;

/**
 * Created by jim per 7/14/14.
 */
public class SslTest {

  public static final String HTTP_SITE = "httpbin.org";
  private static Future<?> submit;
  Tx tx;
  public static final ByteBuffer[] NIL = new ByteBuffer[]{};

  @BeforeClass
  static public void setUp() throws Exception {
    setDEBUG_SENDJSON(true);
    setKillswitch(false);
    submit = RpcHelper.getEXECUTOR_SERVICE().submit(new Runnable() {
      public void run() {
        AsioVisitor topLevel = new ProtocolMethodDispatch();
        try {
          init(null);
        } catch (Exception e) {
          fail();
        }
      }
    });

  }

  static void nukeTestDbs() {
  }

  @Before
  public void before() {
    nukeTestDbs();
  }

  @AfterClass
  static public void tearDown() throws Exception {
    submit.cancel(true);
  }

  @Test
  public void testSSLClient() {
    try {
      final SocketChannel socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(false);
      socketChannel.connect(new InetSocketAddress(HTTP_SITE, 443));
      Server.enqueue(socketChannel, SelectionKey.OP_CONNECT, Helper.toConnect(new F() {

        private String host;
        private int port;

        @Override
        public void apply(final SelectionKey key) throws Exception {
          if (((SocketChannel) key.channel()).finishConnect()) {
            port = 443;
            host = HTTP_SITE;
            final SSLEngine sslEngine = SSLContext.getDefault().createSSLEngine(host, port);
            sslEngine.setUseClientMode(true);
            sslEngine.setEnableSessionCreation(true);


            sslGoal.put(key, Pair.pair(OP_WRITE, (Object) Helper.finishWrite(new Runnable() {
              @Override
              public void run() {
                toRead(key, new F() {
                  @Override
                  public void apply(SelectionKey key) throws Exception {
                    tx = new Tx();
                    int i = tx.readHttpResponse(key);
                    System.err.println(""+tx.toString());
                  }
                });
              }
            }, new Rfc822HeaderState().asRequest().path("/").method(HttpMethod.GET).headerStrings(new LinkedHashMap<String, String>() {{
              put("Host", HTTP_SITE);
            }}).asByteBuffer())));

            final int packetBufferSize = sslEngine.getSession().getPacketBufferSize();
            ByteBuffer toNet = ByteBuffer.allocateDirect(32 << 10);
            needWrap(key, sslEngine, toNet);
          }
        }
      }));
      synchronized (this) {
        this.wait();
      }
    } catch (InterruptedException | IOException e) {
      fail();
      e.printStackTrace();
      e.printStackTrace();
    }

  }


  /**
   * this is a beast.
   *
   * @param key
   * @param sslEngineResult
   * @param sslEngine
   * @param fromNet
   */
  public void beast(final SelectionKey key, final SSLEngineResult sslEngineResult, final SSLEngine sslEngine, final ByteBuffer fromNet) {

    final Status status = sslEngineResult.getStatus();
    switch (status) {
      case BUFFER_UNDERFLOW:
        return;
      case BUFFER_OVERFLOW:
        break;
      case OK:
        final HandshakeStatus handshakeStatus1 = sslEngineResult.getHandshakeStatus();
        switch (handshakeStatus1) {
          case NEED_TASK:
            int i = Helper.delegateTasks(key, sslEngine, getEXECUTOR_SERVICE(), new Runnable() {
              @Override
              public void run() {
                try {
                  beast(key, sslEngine.unwrap(fromNet, ByteBuffer.allocateDirect(sslEngine.getSession().getApplicationBufferSize())), sslEngine,
                      fromNet);
                } catch (SSLException e) {
                  e.printStackTrace();
                }
              }
            });
          case NOT_HANDSHAKING:
          case FINISHED:
            Pair<Integer, Object> integerObjectPair = sslGoal.get(key);
            enqueue(key.channel(), integerObjectPair.getA(),integerObjectPair.getB());
            return;
          case NEED_WRAP:
            needWrap(key, sslEngine, fromNet);
            return;
          case NEED_UNWRAP:
            toRead(key, new F() {
              @Override
              public void apply(SelectionKey key) throws Exception {
                int read = ((SocketChannel) key.channel()).read(fromNet.compact());
                ByteBuffer ignore = ByteBuffer.allocateDirect(32 << 10);//todo: trim
                if (read > 0) {
                  SSLEngineResult unwrap = sslEngine.unwrap((ByteBuffer) fromNet.flip(), ignore);
                  beast(key, unwrap, sslEngine, (ByteBuffer) fromNet);
                }
              }
            });
        }
    }
  }

  public void needWrap(SelectionKey key, SSLEngine sslEngine, ByteBuffer fromNet) {
    try {
      ByteBuffer toNet = ByteBuffer.allocateDirect(32 << 10);//todo: trim
      SSLEngineResult wrap = sslEngine.wrap(NIL, toNet);
      if (wrap.bytesProduced() > 0)
        ((SocketChannel) key.channel()).write((ByteBuffer) toNet.flip());
      beast(key, wrap, sslEngine, fromNet);
    } catch (SSLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}