package rxf.server;

import java.io.*;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import one.xio.AsioVisitor.Impl;
import one.xio.MimeType;

import static java.lang.Math.min;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static rxf.server.CouchMetaDriver.ACCEPT_ENCODING;
import static rxf.server.CouchMetaDriver.CONTENT_ENCODING;
import static rxf.server.CouchMetaDriver.CONTENT_LENGTH;
import static rxf.server.CouchMetaDriver.CONTENT_TYPE;

/**
 * User: jim
 * Date: 6/4/12
 * Time: 1:42 AM
 */
public class ContentRootImpl extends Impl {

  private String rootPath;
  private File absolutePath;

  ContentRootImpl(String rootPath) {
    this.rootPath = rootPath;
    File dir = new File(rootPath);
    absolutePath = dir.getAbsoluteFile();
    if (!dir.isDirectory() && dir.canRead()) throw new IllegalAccessError("can't verify  readable dir at " + rootPath);
  }

  @Override
  public Impl preRead(Object... env) {

    final AtomicReference<Rfc822HeaderState> state = new AtomicReference<Rfc822HeaderState>();
    for (Object o : env) {
      if (o instanceof Rfc822HeaderState) {
        state.set((Rfc822HeaderState) o);
        break;
      }
    }

    String path = state.get().pathResCode();
    String fname = null;
    try {
//      rootPath = ".";

      //      System.err.println("### " + BlobAntiPatternObject.deepToString(path, Pattern.compile("[\\?#]").split(absolutePath.getAbsolutePath() + '/' + path, 2)));

      fname = URLDecoder.decode((Pattern.compile("[\\?#]").split(absolutePath.getAbsolutePath() + '/' + path, 2)[0]).replace("//", "/").replace("../", "./"), UTF8.name());

    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }

    File filex = new File(fname);
    final File[] file = {filex};

    final String finalFname = fname;
//              state.get().sourceKey().selector().wakeup();
    state.get().sourceKey().interestOps(OP_WRITE);
    final SocketChannel channel = (SocketChannel) state.get().sourceKey().channel();
    return (new Impl() {

      @Override
      public void onWrite(SelectionKey key) throws Exception {

        String accepts = state.get().headerString(ACCEPT_ENCODING);
        String ceString = null;
        if (null != accepts) {
//              String accepts = UTF8.decode((ByteBuffer) headers.clear().limit(ints[1]).position(ints[0])).toString().trim();
          for (CompressionTypes compType : CompressionTypes.values()) {
            if (accepts.contains(compType.name())) {
              File file1 = new File(finalFname + "." + compType.suffix);
              if (file1.isFile() && file1.canRead()) {
                file[0] = file1;
                System.err.println("sending compressed archive: " + file1.getAbsolutePath());
                ceString = ("Content-Encoding: " + compType.name() + "\r\n");
                break;
              }
            }
          }
        }
        boolean send200 = false;
        try {
          send200 = file[0].canRead() && file[0].isFile();
        } finally {

        }

        if (send200) {
          final RandomAccessFile randomAccessFile = new RandomAccessFile(file[0], "r");
          final long total = randomAccessFile.length();
          final FileChannel fileChannel = randomAccessFile.getChannel();


          String substring = finalFname.substring(finalFname.lastIndexOf('.') + 1);
          MimeType mimeType = MimeType.valueOf(substring);
          long length = randomAccessFile.length();
          Rfc822HeaderState responseHeader = new Rfc822HeaderState()
              .headerString(CONTENT_TYPE, (null == mimeType ? MimeType.bin : mimeType).contentType)
              .headerString(CONTENT_LENGTH, String.valueOf(length))
              .pathResCode("200")
              /*  .methodProtocol("HTTP/1.1")*/;
          if (null != ceString)
            responseHeader.headerString(CONTENT_ENCODING, ceString);
          ByteBuffer response = responseHeader.asResponseHeaderByteBuffer();
          int write = channel.write(response);
          final int sendBufferSize = BlobAntiPatternObject.getSendBufferSize();
          final long[] progress = {fileChannel.transferTo(0, sendBufferSize, channel)};
          key.interestOps(OP_WRITE | OP_CONNECT);
          key.selector().wakeup();
          key.attach(new Impl() {
            @Override
            public void onWrite(SelectionKey key) throws Exception {
              long remaining = total - progress[0];
              progress[0] += fileChannel.transferTo(progress[0], min(sendBufferSize, remaining), channel);
              remaining = total - progress[0];
              if (0 == remaining) {
                fileChannel.close();
                randomAccessFile.close();
                key.selector().wakeup();
                key.interestOps(OP_READ);
                key.attach(new Object[0]);
              }
            }
          });
        } else {
          key.selector().wakeup();
          key.interestOps(OP_WRITE).attach(new Impl() {

            @Override
            public void onWrite(SelectionKey key) throws Exception {

              String response = "HTTP/1.1 404 Not Found\n" +
                  "Content-Length: 0\n\n";
              System.err.println("!!! " + file[0].getAbsolutePath());
              int write = channel.write(UTF8.encode(response));
              key.selector().wakeup();
              key.interestOps(OP_READ).attach(null);
            }
          });
        }
      }
    });
  }
}
