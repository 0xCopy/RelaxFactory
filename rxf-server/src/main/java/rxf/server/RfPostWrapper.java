package rxf.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.web.bindery.requestfactory.server.ServiceLayer;
import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;

import static java.nio.channels.SelectionKey.OP_READ;
import static rxf.server.CouchMetaDriver.ACCEPT;
import static rxf.server.CouchMetaDriver.CONTENT_ENCODING;
import static rxf.server.CouchMetaDriver.CONTENT_LENGTH;
import static rxf.server.CouchMetaDriver.CONTENT_TYPE;
import static rxf.server.CouchMetaDriver.ETAG;
import static rxf.server.CouchMetaDriver.TRANSFER_ENCODING;

/**
 * a POST interception wrapper for http protocol cracking
 * User: jim
 * Date: 4/18/12
 * Time: 12:37 PM
 */
public class RfPostWrapper extends Impl {

  public static final SimpleRequestProcessor SIMPLE_REQUEST_PROCESSOR = new SimpleRequestProcessor(ServiceLayer.create());
  public static ThreadLocal<Rfc822HeaderState> RFState = new ThreadLocal<Rfc822HeaderState>();

/*
  @Override
  public void onRead(final SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    ByteBuffer dst = ByteBuffer.allocateDirect(BlobAntiPatternObject.getReceiveBufferSize());
    int read = 0;
    try {
      if (channel.socket().isInputShutdown() || channel.socket().isOutputShutdown() || !channel.isConnected()) {
        key.cancel();
        return;
      }
      read = channel.read(dst);
    } catch (IOException e) {
      channel.close();
    }
    if (-1 == read) {
      channel.close();
    } else {
      dst.flip();
      ByteBuffer duplicate = dst.duplicate();
      while (duplicate.hasRemaining() && !Character.isWhitespace(duplicate.get())) ;
      String trim = UTF8.decode((ByteBuffer) duplicate.flip()).toString().trim();
      HttpMethod method = HttpMethod.valueOf(trim);
      dst.limit(read).position(0);
      key.attach(dst);
      switch (method) {
        case POST: {
          BlobAntiPatternObject.moveCaretToDoubleEol(dst);
          ByteBuffer headers = (ByteBuffer) dst.duplicate().flip();
          if (DEBUG_SENDJSON) {
            System.err.println("### headers: " + UTF8.decode((ByteBuffer) headers.duplicate().rewind()).toString());
          }
          Map<String, int[]> headers1 = HttpHeaders.getHeaders(headers);
          int[] ints = headers1.get("Content-Length");


          ByteBuffer duplicate1 = (ByteBuffer) headers.duplicate().rewind();
          String trim1 = UTF8.decode((ByteBuffer) duplicate1.limit(ints[1]).position(ints[0])).toString().trim();
          long total = Long.parseLong(trim1);

          long[] remaining = {total - dst.remaining()};
          if (0 == remaining[0]) {
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new RfProcessTask(headers, dst, key));
          } else {
            if (dst.capacity() - dst.position() >= total) {
              headers = ByteBuffer.allocateDirect(dst.position()).put(headers);
              //alert: buhbye headers
              dst.compact().limit((int) total);

            } else {
              //noinspection NumericCastThatLosesPrecision
              dst = ByteBuffer.allocateDirect((int) total).put(dst);
            }
            final ByteBuffer finalDst = dst;
            final ByteBuffer finalHeaders = headers;
            key.attach(new Impl() {
              @Override
              public void onRead(SelectionKey selectionKey) throws IOException {
                ((SocketChannel) selectionKey.channel()).read(finalDst);
                if (!finalDst.hasRemaining()) {
                  EXECUTOR_SERVICE.submit(new RfProcessTask(finalHeaders, finalDst, key));
                }
              }
            });
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
          for (Map.Entry<Pattern, AsioVisitor> visitorEntry : BlobAntiPatternObject.getNamespace().get(method).entrySet()) {
            final Matcher matcher = visitorEntry.getKey().matcher(path);
            final boolean b = matcher.find();
            if (b) {

              key.selector().wakeup();
              key.interestOps(OP_CONNECT | OP_WRITE).attach(
                  */

  /**
   * sends impl,path,headers,first block
   *//*
                  new Object[]{visitorEntry.getValue(), path, headers, dst.rewind()});
              key.selector().wakeup();
              return;
            }
          }

          //message.format works well for splits
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
                *//* try {
              length = filex.length();
            } catch (Exception e) *//*
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
  }
 */
  @Override
  public void onAccept(SelectionKey key) throws IOException {
    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
    SocketChannel accept = channel.accept();
    accept.configureBlocking(false);
    HttpMethod.enqueue(accept, OP_READ, this);

  }
/*

  public static class RfProcessTask implements Runnable {
    private final ByteBuffer headers;
    private final ByteBuffer data;
    SelectionKey key;

    public RfProcessTask(ByteBuffer headers, ByteBuffer data, SelectionKey key) {
      this.headers = headers;
      this.data = data;
      this.key = key;

    }


    public void run() {
      BlobAntiPatternObject.ThreadLocalHeaders.set(headers);
      SocketChannel socketChannel = (SocketChannel) key.channel();
      InetAddress remoteSocketAddress = socketChannel.socket().getInetAddress();
      BlobAntiPatternObject.ThreadLocalInetAddress.set(remoteSocketAddress);
      String trim = UTF8.decode(data).toString().trim();
      final String process;
      try {
        process = SIMPLE_REQUEST_PROCESSOR.process(trim);
        String sc = setOutboundCookies();
        int length = process.length();
        final String s1 = "HTTP/1.1 200 OK\r\n" +
            sc +
            "Content-Type: application/json ; charset=utf-8\r\n" +
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
*/
/*
    public String setOutboundCookies() {
      System.err.println("+++ headers " + UTF8.decode((ByteBuffer) headers.rewind()).toString());
      Map setCookiesMap = BlobAntiPatternObject.ThreadLocalSetCookies.get();
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
    }*/

  @Override
  public void onRead(SelectionKey key) throws Exception {
    final SocketChannel channel = (SocketChannel) key.channel();

    ByteBuffer cursor = ByteBuffer.allocateDirect(BlobAntiPatternObject.getReceiveBufferSize());
    int read = channel.read(cursor);
    if (-1 == read) {
      ((SocketChannel) key.channel()).socket().close();//cancel();
      return;
    }
    //break down the incoming headers.
    final Rfc822HeaderState state;
    RFState.set(state = new Rfc822HeaderState(CONTENT_LENGTH, CONTENT_TYPE, CONTENT_ENCODING, ETAG, TRANSFER_ENCODING, ACCEPT).sourceKey(key).apply((ByteBuffer) cursor.flip()));


    //find the method to dispatch
    HttpMethod method = HttpMethod.valueOf(state.methodProtocol());

    if (null == method) {
      ((SocketChannel) key.channel()).socket().close();//cancel();

      return;
    }
    //check for namespace registration
    // todo: preRead is  wierd initiailizer which needs some review.
    for (Entry<Pattern, Impl> visitorEntry : BlobAntiPatternObject.getNamespace().get(method).entrySet()) {
      Matcher matcher = visitorEntry.getKey().matcher(state.pathResCode());
      if (matcher.find()) {
        Impl impl = visitorEntry.getValue();

        Impl ob = impl.preRead(state, cursor);
        if (null != ob) {
          key.attach(ob);
//        visitorEntry.getValue().onRead(key);
          key.selector().wakeup();
        }
        return;
      }
    }
    switch (method) {
      default:
        throw new Error(BlobAntiPatternObject.arrToString("unknown method in", state));
    }
  }


}

