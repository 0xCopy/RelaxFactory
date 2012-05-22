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
import java.util.WeakHashMap;
import java.util.concurrent.Exchanger;
import java.util.regex.Pattern;

import com.google.web.bindery.requestfactory.server.ExceptionHandler;
import com.google.web.bindery.requestfactory.server.ServiceLayer;
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

/**
 * a POST interception wrapper for http protocol cracking
 * User: jim
 * Date: 4/18/12
 * Time: 12:37 PM
 */
public class RfPostWrapper extends Impl {

  public static final ServiceLayerDecorator SERVICE_LAYER_DECORATOR = new ServiceLayerDecorator();
  public static final SimpleRequestProcessor SIMPLE_REQUEST_PROCESSOR = new SimpleRequestProcessor(ServiceLayer.create(SERVICE_LAYER_DECORATOR));
  public static final String CONTENT_LENGTH = "Content-Length";
  public static final Map<SelectionKey, Rfc822HeaderState> ORIGINS = $DBG ? new WeakHashMap<SelectionKey, Rfc822HeaderState>() : null;

  static {
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
    final ByteBuffer dst = ByteBuffer.allocateDirect(BlobAntiPatternObject.getReceiveBufferSize());
    final SocketChannel channel = (SocketChannel) key.channel();
    int read = channel.read(dst);
    if (read < 0) {
      if ($DBG && ORIGINS.containsKey(key)) {
        System.err.println($DBG + ": closing " + ORIGINS.get(key).getPathRescode());
      }
      channel.close();
    } else {
      if (read > 0) {
        final Rfc822HeaderState rfc822HeaderState = new Rfc822HeaderState(CONTENT_LENGTH).apply((ByteBuffer) dst.flip()).cookies(BlobAntiPatternObject.MYGEOIPSTRING, BlobAntiPatternObject.class.getCanonicalName()).sourceKey(key);
        ThreadLocalHeaders.set(rfc822HeaderState);
        HttpMethod method = HttpMethod.valueOf(rfc822HeaderState.getMethodProtocol());
        if ($DBG) {
          ORIGINS.put(key, rfc822HeaderState);
        }
        if (null != method) {
          switch (method) {
            case POST: {

              EXECUTOR_SERVICE.submit(new Runnable() {
                public void run() {
                  ThreadLocalHeaders.set(rfc822HeaderState);

                  final Exchanger<ByteBuffer> exchanger = new Exchanger<ByteBuffer>();
                  final Object o = rfc822HeaderState.getHeaderStrings().get(RfPostWrapper.CONTENT_LENGTH);
                  int remaining = Integer.parseInt((String) o);

                  final ByteBuffer cursor = ByteBuffer.allocateDirect(remaining).put(dst);
                  EXECUTOR_SERVICE.submit(new Runnable() {
                    public void run() {
                      ThreadLocalHeaders.set(rfc822HeaderState);
                      try {
                        if (cursor.hasRemaining()) key.interestOps(OP_READ).attach(new Impl() {
                          @Override
                          public void onRead(final SelectionKey key) throws Exception {
                            channel.read(cursor);
                            if (!cursor.hasRemaining()) {
                              /////////////////////////////////////////////////////////////////////////////////////////////////////

                              exchanger.exchange(cursor);
                              key.interestOps(0);
                            }
                          }
                        });        //////
                        exchanger.exchange(cursor);
                      } catch (Throwable e) {
                        e.printStackTrace();  //
                      }
                    }
                  });
                  try {
                    {
                      ByteBuffer dst = exchanger.exchange(null);

                      String trim = UTF8.decode((ByteBuffer) dst.flip()).toString().trim();
                      System.err.println("exchanger says: " + UTF8.decode((ByteBuffer) dst.duplicate().rewind()));
                      InetAddress remoteSocketAddress = channel.socket().getInetAddress();
                      String process = null;
                      try {
                        try {
                          ThreadLocalHeaders.set(rfc822HeaderState);
                          //                        ThreadLocalInetAddress.set(remoteSocketAddress);
                          //              SERVICE_LAYER.
                          process = SIMPLE_REQUEST_PROCESSOR.process(trim);
                          System.err.println("+++ headers " + UTF8.decode((ByteBuffer) rfc822HeaderState.getHeaderBuf().rewind()).toString());

                          String sc1 = "";
                          if (rfc822HeaderState.isDirty()) {
                            sc1 = "";

                            final Map<String, String> rfc822HeaderStateCookieStrings = rfc822HeaderState.getCookieStrings();
                            Iterator<Map.Entry<String, String>> iterator = rfc822HeaderStateCookieStrings.entrySet().iterator();
                            if (iterator.hasNext()) {
                              do {
                                Map.Entry<String, String> stringStringEntry = iterator.next();
                                sc1 += "Set-Cookie: " + stringStringEntry.getKey() + "=" + stringStringEntry.getValue().trim();
//                                if (iterator.hasNext()) sc1 += "; ";
                                sc1 += "\r\n";
                              } while (iterator.hasNext());
                            }

                          }
                          int length = process.length();
                          final String s1 = "HTTP/1.1 200 OK\r\n" +
                              sc1 +
                              "Content-Type: application/json\r\n" +
                              "Content-Length: " + length + "\r\n\r\n";
                          final String finalProcess = process;
                          key.interestOps(OP_WRITE).attach(new Impl() {

                            private ByteBuffer payload = UTF8.encode(s1 + finalProcess);

                            @Override
                            public void onWrite(SelectionKey selectionKey) throws IOException {
                              channel.write(payload);
                              if (!payload.hasRemaining()) {
                                key.interestOps(OP_READ).attach(null);
                              }
                            }
                          });
                        } finally {

                        }

                      } catch (Throwable e) {
                        e.printStackTrace();  //
                      }

                    }
                  } catch (InterruptedException e) {
                    e.printStackTrace();  //
                  }
                }
              });
              return;
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
                if (visitorEntry.getKey().matcher(path).find()) {
                  ThreadLocalHeaders.set(rfc822HeaderState);

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
                  ThreadLocalHeaders.set(rfc822HeaderState);

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

                        String response = "HTTP/1.1 404 Not Found\n\n";
                        int write = socketChannel.write(UTF8.encode(response));
                        key.channel().close();
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
              if ($DBG) {
                final Rfc822HeaderState rfc822HeaderState1 = ORIGINS.get(key);
                final String pathRescode = rfc822HeaderState1.getPathRescode();
                System.err.println("going down in flames:" + pathRescode);
                channel.close();
              }
              break;
          }
        } else {

          key.cancel();
        }
      }
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

}

