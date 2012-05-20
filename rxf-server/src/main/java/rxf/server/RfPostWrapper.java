package rxf.server;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Exchanger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.web.bindery.requestfactory.server.ExceptionHandler;
import com.google.web.bindery.requestfactory.server.ServiceLayerDecorator;
import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import one.xio.AsioVisitor;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import one.xio.MimeType;

import static java.lang.Math.min;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.wheresWaldo;
import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.ThreadLocalHeaders;
import static rxf.server.BlobAntiPatternObject.ThreadLocalInetAddress;

/**
 * a POST interception wrapper for http protocol cracking
 * User: jim
 * Date: 4/18/12
 * Time: 12:37 PM
 */
public class RfPostWrapper extends Impl {

  public static final ServiceLayerDecorator SERVICE_LAYER = new ServiceLayerDecorator();
  public static final SimpleRequestProcessor SIMPLE_REQUEST_PROCESSOR = new SimpleRequestProcessor(SERVICE_LAYER);
  public static final String CONTENT_LENGTH = "Content-Length";

  {
    SIMPLE_REQUEST_PROCESSOR.setExceptionHandler(new ExceptionHandler() {
      @Override
      public ServerFailure createServerFailure(Throwable throwable) {
        throwable.fillInStackTrace();
        System.err.println("BOOM! in rfpw");
        throwable.printStackTrace();
        return new ServerFailure(wheresWaldo());
      }
    });
  }

  @Override
  public void onRead(final SelectionKey key) throws IOException {
    ByteBuffer dst = ByteBuffer.allocateDirect(BlobAntiPatternObject.getReceiveBufferSize());
    final SocketChannel channel = (SocketChannel) key.channel();
    int read = channel.read(dst);


    final Rfc822HeaderState rfc822HeaderState = new Rfc822HeaderState(CONTENT_LENGTH).apply((ByteBuffer) dst.flip()).cookies(BlobAntiPatternObject.MYGEOIPSTRING, BlobAntiPatternObject.class.getCanonicalName());

    HttpMethod method = HttpMethod.valueOf(rfc822HeaderState.getMethodProtocol());
    if (null == method) {
      key.cancel();
    }
    switch (method) {
      case POST: {

        final Exchanger<ByteBuffer> exchanger = new Exchanger<ByteBuffer>();
        final Object o = rfc822HeaderState.getHeaderStrings().get(RfPostWrapper.CONTENT_LENGTH);
        int remaining = Integer.parseInt((String) o);

        final ByteBuffer cursor = ByteBuffer.allocateDirect(remaining).put(dst);
        EXECUTOR_SERVICE.submit(new Callable<Void>() {
          public Void call() throws Exception {
            if (cursor.hasRemaining()) key.interestOps(OP_READ).attach(new Impl() {
              @Override
              public void onRead(final SelectionKey key) throws Exception {
                channel.read(cursor);
                if (!cursor.hasRemaining()) {
                  /////////////////////////////////////////////////////////////////////////////////////////////////////

                  exchanger.exchange(cursor);
                  return;

                }
              }
            });        //////
            exchanger.exchange(cursor);
            return null;
          }
        });
        try {
          dst = exchanger.exchange(null);

          String trim = UTF8.decode((ByteBuffer) dst.flip()).toString().trim();
          System.err.println("exchanger says: " + UTF8.decode((ByteBuffer) dst.duplicate().rewind()));
          InetAddress remoteSocketAddress = channel.socket().getInetAddress();
            String process = null;
          try {
            try {
          ThreadLocalHeaders.set(rfc822HeaderState);
          ThreadLocalInetAddress.set(remoteSocketAddress);
              SERVICE_LAYER.
              process = SIMPLE_REQUEST_PROCESSOR.process(trim);
            } catch (RuntimeException e) {
              e.printStackTrace();  //todo: verify for a purpose
            } finally {
            }
            System.err.println("+++ headers " + UTF8.decode((ByteBuffer) rfc822HeaderState.getHeaderBuf().rewind()).toString());
            Map<String, String> setCookiesMap = BlobAntiPatternObject.ThreadLocalSetCookies.get();
            String sc1 = "";
            if (null != setCookiesMap && !setCookiesMap.isEmpty()) {
              sc1 = "";

              Iterator<Map.Entry<String, String>> iterator = setCookiesMap.entrySet().iterator();
              if (iterator.hasNext()) {
                do {
                  Map.Entry<String, String> stringStringEntry = iterator.next();
                  sc1 += "Set-Cookie: " + stringStringEntry.getKey() + "=" + stringStringEntry.getValue().trim();
                  if (iterator.hasNext()) sc1 += "; ";
                  sc1 += "\r\n";
                } while (iterator.hasNext());
              }

            }
            String sc = sc1;
            int length = process.length();
            final String s1 = "HTTP/1.1 200 OK\r\n" +
                sc +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + length + "\r\n\r\n";
            final String finalProcess = process;
            key.attach(new Impl() {
              @Override
              public void onWrite(SelectionKey selectionKey) throws IOException {
                channel.write(UTF8.encode(s1 + finalProcess));
                System.err.println("debug: " + s1 + finalProcess);
                key.attach(null);
                key.selector().wakeup();
                key.interestOps(OP_READ);
              }
            });
            key.selector().wakeup();
            key.interestOps(SelectionKey.OP_WRITE);

          } catch (Throwable e) {
            e.printStackTrace();  //todo: verify for a purpose
          } finally {
          }

        } catch (Throwable e) {
          e.printStackTrace();  //todo: verify for a purpose
        }

        break;
      }
      case GET: {
        BlobAntiPatternObject.moveCaretToDoubleEol(dst);
        final ByteBuffer headers = ((ByteBuffer) dst.duplicate().flip()).slice();
        while (!Character.isWhitespace(headers.get())) ;
        int position = headers.position();
        while (!Character.isWhitespace(headers.get())) ;

        String path = URLDecoder.decode(UTF8.decode((ByteBuffer) headers.flip().position(position)).toString().trim());
        LinkedHashMap<Pattern, AsioVisitor> patternAsioVisitorLinkedHashMap = BlobAntiPatternObject.getNamespace().get(method);
        if (null == patternAsioVisitorLinkedHashMap) {
          patternAsioVisitorLinkedHashMap = new LinkedHashMap<Pattern, AsioVisitor>();
          BlobAntiPatternObject.getNamespace().put(method, patternAsioVisitorLinkedHashMap);
        }              //should also be for POST
        final Set<Map.Entry<Pattern, AsioVisitor>> entries = patternAsioVisitorLinkedHashMap.entrySet();
        for (Map.Entry<Pattern, AsioVisitor> visitorEntry : entries) {
          final Matcher matcher = visitorEntry.getKey().matcher(path);
          final boolean b = matcher.find();
          if (b) {

            key.selector().wakeup();
            key.interestOps(OP_CONNECT | OP_WRITE).attach(
                /**sends impl,path,headers,first block
                 */
                new Object[]{visitorEntry.getValue(), path, headers, dst.rewind()});
            key.selector().wakeup();
            return;
          }
        }
        String fname = MessageFormat.format("./{0}", path.split("[\\#\\?]")).replace("//", "/").replace("../", "./");

        final File filex = new File(fname);
        final File[] file = {filex};

        final String finalFname = fname;
        key.selector().wakeup();
        key.interestOps(OP_WRITE);
        key.attach(new Impl() {

          @Override
          public void onWrite(SelectionKey key) throws Exception {
            final SocketChannel socketChannel = (SocketChannel) key.channel();
            final int sendBufferSize = BlobAntiPatternObject.getSendBufferSize();
            String ceString = "";

            Map<String, int[]> hmap = HttpHeaders.getHeaders((ByteBuffer) headers.clear());
            int[] ints = hmap.get("Accept-Encoding");
            if (null != ints) {
              String accepts = UTF8.decode((ByteBuffer) headers.clear().limit(ints[1]).position(ints[0])).toString().trim();
              for (CompressionTypes compType : CompressionTypes.values()) {
                if (accepts.contains(compType.name())) {
                  File file1 = new File(finalFname + "." + compType.suffix);
                  if (file1.isFile()) {
                    file[0] = file1;
                    System.err.println("sending compressed archive: " + file1.getAbsolutePath());
                    ceString = MessageFormat.format("Content-Encoding: {0}\r\n", compType.name());
                    break;
                  }
                }
              }
            }
            boolean send200 = false;
            try {
              send200 = file[0].canRead();
            } finally {

            }

            if (!send200) {
              key.selector().wakeup();
              key.interestOps(OP_WRITE).attach(new Impl() {
                @Override
                public void onWrite(SelectionKey key) throws Exception {

                  String response = "HTTP/1.1 404 Not Found\n" +
                      "Content-Length: 0\n\n";
                  int write = socketChannel.write(UTF8.encode(response));
                  key.selector().wakeup();
                  key.interestOps(OP_READ).attach(RfPostWrapper.this);
                  key.selector().wakeup();
                }
              });
            } else {
              final RandomAccessFile randomAccessFile = new RandomAccessFile(file[0], "r");
              final long total = randomAccessFile.length();
              final FileChannel fileChannel = randomAccessFile.getChannel();


              String substring = finalFname.substring(finalFname.lastIndexOf('.') + 1);
              MimeType mimeType = MimeType.valueOf(substring);
              long length;
              /* try {
            length = filex.length();
          } catch (Exception e) */
              {
                length = randomAccessFile.length();
              }
              String response = MessageFormat.format("HTTP/1.1 200 OK\r\nContent-Type: {0}\r\nContent-Length: {1,number,#}\r\n{2}\r\n", (null == mimeType ? MimeType.bin : mimeType).contentType, length, ceString);
              int write = socketChannel.write(UTF8.encode(response));
              final long[] progress = {fileChannel.transferTo(0, sendBufferSize, socketChannel)};
              key.selector().wakeup();
              key.interestOps(OP_WRITE | OP_CONNECT);
              key.attach(new Impl() {
                @Override
                public void onWrite(SelectionKey key) throws Exception {
                  long remaining = total - progress[0];
                  progress[0] += fileChannel.transferTo(progress[0], min(sendBufferSize, remaining), socketChannel);
                  remaining = total - progress[0];
                  if (remaining == 0) {
                    fileChannel.close();
                    randomAccessFile.close();
                    key.selector().wakeup();
                    key.interestOps(OP_READ);
                    key.attach(new Object[0]);
                  }
                }
              });
            }
          }
        });
        break;
      }
      default:
        method.onRead(key);
        break;
    }
  }


  public void onConnect(SelectionKey key) {
    HttpMethod.$.onConnect(key);         // hugely deprecated
  }


  @Override
  public void onAccept(SelectionKey key) throws IOException {
    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
    SocketChannel accept = channel.accept();
    accept.configureBlocking(false);
    HttpMethod.enqueue(accept, OP_READ, this);

  }

  public static class RfProcessTask implements Runnable {
    private final ByteBuffer headers;
    private final ByteBuffer data;
    SelectionKey key;

    public RfProcessTask(ByteBuffer headers, ByteBuffer data, SelectionKey key) {
      this.headers = headers;
      this.data = data;
      this.key = key;

    }

    @Override
    public void run() {
      SocketChannel socketChannel = (SocketChannel) key.channel();
      InetAddress remoteSocketAddress = socketChannel.socket().getInetAddress();
      ThreadLocalInetAddress.set(remoteSocketAddress);
      String trim = UTF8.decode(data).toString().trim();
      final String process;
      try {
        process = SIMPLE_REQUEST_PROCESSOR.process(trim);
        String sc = setOutboundCookies();
        int length = process.length();
        final String s1 = "HTTP/1.1 200 OK\r\n" +
            sc +
            "Content-Type: application/json\r\n" +
            "Content-Length: " + length + "\r\n\r\n";
        key.attach(new AsioVisitor.Impl() {
          @Override
          public void onWrite(SelectionKey selectionKey) throws IOException {
            ((SocketChannel) key.channel()).write(UTF8.encode(s1 + process));
            System.err.println("debug: " + s1 + process);
            key.attach(null);
            key.selector().wakeup();
            key.interestOps(OP_READ);
          }
        });
        key.selector().wakeup();
        key.interestOps(SelectionKey.OP_WRITE);

      } catch (Throwable e) {
        e.printStackTrace();  //todo: verify for a purpose
      } finally {
      }

    }

    String setOutboundCookies() {
      System.err.println("+++ headers " + UTF8.decode((ByteBuffer) headers.rewind()).toString());
      Map<String, String> setCookiesMap = BlobAntiPatternObject.ThreadLocalSetCookies.get();
      String sc = "";
      if (null != setCookiesMap && !setCookiesMap.isEmpty()) {
        sc = "";

        Iterator<Map.Entry<String, String>> iterator = setCookiesMap.entrySet().iterator();
        if (iterator.hasNext()) {
          do {
            Map.Entry<String, String> stringStringEntry = iterator.next();
            sc += "Set-Cookie: " + stringStringEntry.getKey() + "=" + stringStringEntry.getValue().trim();
            if (iterator.hasNext()) sc += "; ";
            sc += "\r\n";
          } while (iterator.hasNext());
        }

      }
      return sc;
    }
  }
}

