import one.xio.AsioVisitor;
import one.xio.AsioVisitor.Helper.*;
import one.xio.HttpMethod;
import one.xio.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rxf.core.Rfc822HeaderState;
import rxf.core.Server;
import rxf.core.Tx;
import rxf.web.inf.ProtocolMethodDispatch;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.concurrent.Future;

import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.AsioVisitor.Helper.*;
import static org.junit.Assert.fail;
import static rxf.core.Server.init;
import static rxf.core.Server.setKillswitch;
import static rxf.rpc.RpcHelper.getEXECUTOR_SERVICE;
import static rxf.rpc.RpcHelper.setDEBUG_SENDJSON;

/**
 * Created by jim per 7/14/14.
 *
 * http://stackoverflow.com/questions/23324807/randomly-sslexception-unsupported-record-version-unknown-0-0 matters
 *
 */
public class SslTest {

//  public static final String HTTP_SITE = "omgrentbbq.appspot.com";
  public static final String HTTP_SITE = "httpbin.org";
  private static Future<?> submit;
  Tx tx;
  SSLException dammit;
  @BeforeClass
  static public void setUp() throws Exception {
    setDEBUG_SENDJSON(true);
    setKillswitch(false);
    setExecutorService(getEXECUTOR_SERVICE());//one-time installation
    submit = getEXECUTOR_SERVICE().submit(new Runnable() {
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
  public void testSSLClient() throws IOException {

/*
    */
//      @SuppressWarnings("UnnecessaryFullyQualifiedName") Object content = new java.net.URL("https://" +HTTP_SITE).getContent();


    try {
      final SocketChannel socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(false);
      socketChannel.connect(new InetSocketAddress(HTTP_SITE, 443));
      Server.enqueue(socketChannel, SelectionKey.OP_CONNECT, toConnect(new F() {

        private String host;
        private int port;


        public void apply(final SelectionKey key) throws Exception {
          if (((SocketChannel) key.channel()).finishConnect()) {
            port = 443;
            host = HTTP_SITE;
            SSLContext sslContext = SSLContext.getInstance("TLS");

            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
              @Override
              public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

              }

              @Override
              public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

              }

              @Override
              public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
              }
            }}, null);
            sslContext.setDefault(sslContext);


            final SSLEngine sslEngine = sslContext
                .createSSLEngine(host, port);
            sslEngine.setUseClientMode(true);
            sslEngine.setNeedClientAuth(false);
            sslEngine.setWantClientAuth(false);

            ByteBuffer host1 = new Rfc822HeaderState()//
                .asRequest()//
                .path("/")//
                .method(HttpMethod.GET)//
                .headerStrings(new LinkedHashMap<String, String>() {{//
                  put("Host", HTTP_SITE);
                  put("Connection", "keep-alive");
                  put("User-Agent", "Java/1xio");

                }}).asByteBuffer();
            sslGoal.put(key, Pair.pair(OP_WRITE, (Object) finishWrite(new Runnable() {

              public void run() {
                toRead(key, new F() {

                  public void apply(SelectionKey key) throws Exception {
                    tx = new Tx();
                    int i = tx.readHttpResponse(key);
                    System.err.println("" + tx.toString());
                  }
                });
              }
            }, Rfc822HeaderState.avoidStarvation(host1))));

            final int packetBufferSize = sslEngine.getSession().getPacketBufferSize();
            ByteBuffer toNet = ByteBuffer.allocateDirect(32 << 10);
            needWrap(Pair.pair(key, sslEngine));
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
}