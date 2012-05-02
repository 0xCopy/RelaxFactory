package ro.server;

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
import java.util.Map;

import com.google.web.bindery.requestfactory.server.ServiceLayer;
import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor;
import one.xio.AsioVisitor;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import one.xio.MimeType;

import static java.lang.Math.min;
import static one.xio.HttpMethod.UTF8;

/**
 * a POST interception wrapper for http protocol cracking
 * User: jim
 * Date: 4/18/12
 * Time: 12:37 PM
 */
class RfPostWrapper extends Impl {
  public static final SimpleRequestProcessor SIMPLE_REQUEST_PROCESSOR = new SimpleRequestProcessor(ServiceLayer.create());

  @Override
  public void onRead(final SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    ByteBuffer dst = ByteBuffer.allocate(KernelImpl.getReceiveBufferSize());
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
          KernelImpl.moveCaretToDoubleEol(dst);
          ByteBuffer headers = (ByteBuffer) dst.duplicate().flip();
          System.err.println("+++ headers: " + UTF8.decode((ByteBuffer) headers.duplicate().rewind()).toString());
          Map<String, int[]> headers1 = HttpHeaders.getHeaders(headers);
          int[] ints = headers1.get("Content-Length");


          ByteBuffer duplicate1 = (ByteBuffer) headers.duplicate().rewind();
          String trim1 = UTF8.decode((ByteBuffer) duplicate1.limit(ints[1]).position(ints[0])).toString().trim();
          long total = Long.parseLong(trim1);

          long[] remaining = {total - dst.remaining()};
          if (0 == remaining[0]) {
            KernelImpl.EXECUTOR_SERVICE.submit(new RfProcessTask(headers, dst, key));
          } else {
            if (dst.capacity() - dst.position() >= total) {
              headers = ByteBuffer.allocate(dst.position()).put(headers);
              //alert: buhbye headers
              dst.compact().limit((int) total);

            } else {
              dst = ByteBuffer.allocate((int) total).put(dst);
            }
            final ByteBuffer finalDst = dst;
            final ByteBuffer finalHeaders = headers;
            key.attach(new Impl() {
              @Override
              public void onRead(SelectionKey selectionKey) throws IOException {
                ((SocketChannel) selectionKey.channel()).read(finalDst);
                if (!finalDst.hasRemaining()) {
                  KernelImpl.EXECUTOR_SERVICE.submit(new RfProcessTask(finalHeaders, finalDst, key));
                }
              }
            });
          }
          break;
        }
        case GET: {
          KernelImpl.moveCaretToDoubleEol(dst);
          final ByteBuffer headers = ((ByteBuffer) dst.duplicate().flip()).slice();
          while (!Character.isWhitespace(headers.get())) ;
          int position = headers.position();
          while (!Character.isWhitespace(headers.get())) ;
          String fname = URLDecoder.decode(UTF8.decode((ByteBuffer) headers.flip().position(position)).toString().trim());

          fname = MessageFormat.format("./{0}", fname.split("[\\#\\?]")).replace("//", "/").replace("../", "./");
          final File filex = new File(fname);
          final File[] file = {filex};
          if (file[0].isFile()) {
            final String finalFname = fname;
            key.interestOps(SelectionKey.OP_WRITE);
            key.attach(new Impl() {

              @Override
              public void onWrite(SelectionKey key) throws Exception {


                final SocketChannel socketChannel = (SocketChannel) key.channel();
                final int sendBufferSize = KernelImpl.getSendBufferSize();
                String ceString = "";

                Map<String, int[]> hmap = HttpHeaders.getHeaders((ByteBuffer) headers.clear());
                int[] ints = hmap.get("Accept-Encoding");
                if (null != ints) {
                  String accepts = UTF8.decode((ByteBuffer) headers.clear().limit(ints[1]).position(ints[0])).toString().trim();
                  for (CompressionTypes compTypes : CompressionTypes.values()) {
                    if (accepts.contains(compTypes.name())) {
                      File file1 = new File(finalFname + "." + compTypes.suffix);
                      if (file1.isFile()) {
                        file[0] = file1;
                        System.err.println("sending compressed archive: " + file1.getAbsolutePath());
                        ceString = MessageFormat.format("Content-Encoding: {0}\r\n", compTypes.name());
                        break;
                      }
                    }
                  }
                }

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
//                System.err.println("GET response: " + response + '+');


                int write = socketChannel.write(UTF8.encode(response));

                final long[] progress = {fileChannel.transferTo(0, sendBufferSize, socketChannel)};
                key.interestOps(SelectionKey.OP_WRITE);
                key.attach(new Impl() {
                  @Override
                  public void onWrite(SelectionKey key) throws Exception {
                    long remaining = total - progress[0];
                    progress[0] += fileChannel.transferTo(progress[0], min(sendBufferSize, remaining), socketChannel);
                    remaining = total - progress[0];
                    if (remaining == 0) {
                      fileChannel.close();
                      randomAccessFile.close();
                      key.interestOps(SelectionKey.OP_READ);
                      key.attach(new Object[0]);
                    }
                  }
                });
              }
            });
          }
          break;
        }
        default:
          method.onRead(key);
          break;
      }
    }
  }


  public void onConnect(SelectionKey key) {
    HttpMethod.$.onConnect(key);
  }


  @Override
  public void onAccept(SelectionKey key) throws IOException {
    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
    SocketChannel accept = channel.accept();
    accept.configureBlocking(false);
    HttpMethod.enqueue(accept, SelectionKey.OP_READ, this);

  }

  private static class RfProcessTask implements Runnable {
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
      KernelImpl.ThreadLocalHeaders.set(headers);
      SocketChannel socketChannel = (SocketChannel) key.channel();
      InetAddress remoteSocketAddress = socketChannel.socket().getInetAddress();
      KernelImpl.ThreadLocalInetAddress.set(remoteSocketAddress);
      String trim = UTF8.decode(data).toString().trim();
      final String process = SIMPLE_REQUEST_PROCESSOR.process(trim);
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
          key.interestOps(SelectionKey.OP_READ);
        }
      });
      key.interestOps(SelectionKey.OP_WRITE);

    }

    String setOutboundCookies() {
      System.err.println("+++ headers " + UTF8.decode((ByteBuffer) headers.rewind()).toString());
      Map setCookiesMap = KernelImpl.ThreadLocalSetCookies.get();
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

