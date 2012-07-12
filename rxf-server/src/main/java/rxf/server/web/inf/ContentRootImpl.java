package rxf.server.web.inf;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpStatus;
import one.xio.MimeType;
import rxf.server.*;
import rxf.server.Rfc822HeaderState.HttpRequest;
import rxf.server.Rfc822HeaderState.HttpResponse;

import static java.lang.Math.min;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpHeaders.Accept$2dEncoding;
import static one.xio.HttpHeaders.Content$2dEncoding;
import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.Content$2dType;
import static one.xio.HttpMethod.UTF8;
import static rxf.server.BlobAntiPatternObject.HEADER_TERMINATOR;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;
import static rxf.server.BlobAntiPatternObject.scrub;

/**
 * User: jim
 * Date: 6/4/12
 * Time: 1:42 AM
 */
public class ContentRootImpl extends Impl implements PreRead {

  private String rootPath /*= CouchNamespace.COUCH_DEFAULT_FS_ROOT*/;
  private ByteBuffer cursor;
  private HttpRequest request;
  private SocketChannel channel;
  private HttpRequest req;
  File file;

  public ContentRootImpl() {
    this.rootPath = CouchNamespace.COUCH_DEFAULT_FS_ROOT;


    init();
  }


  public ContentRootImpl(String[] rootPath) {
//        this.rootPath = rootPath;
    for (String s : rootPath) {
      this.rootPath = s;
    }

    init();
  }

  public void init() {
    File dir = new File(rootPath);
    if (!dir.isDirectory() && dir.canRead())
      throw new IllegalAccessError("can't verify  readable dir at " + rootPath);
  }

  @Override
  public void onRead(SelectionKey key) throws Exception {
    channel = (SocketChannel) key.channel();
    if (cursor == null) {
      if (key.attachment() instanceof Object[]) {
        Object[] ar = (Object[]) key.attachment();
        for (Object o : ar) {
          if (o instanceof ByteBuffer) {
            cursor = (ByteBuffer) o;
            continue;
          }
          if (o instanceof Rfc822HeaderState) {
            req = ((Rfc822HeaderState) o).$req();
            continue;
          }
        }
      }
      key.attach(this);
    }
    cursor = null == cursor ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : cursor.hasRemaining() ? cursor : ByteBuffer.allocateDirect(cursor.capacity() << 1).put((ByteBuffer) cursor.rewind());
    int read = channel.read(cursor);
    if (read == -1)
      key.cancel();
    Buffer flip = cursor.duplicate().flip();
    req = (HttpRequest) ActionBuilder.get().state().$req().apply((ByteBuffer) flip);
    if (!BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, req.headerBuf())) {
      return;
    }
    cursor = ((ByteBuffer) flip).slice();
    /*  int remaining = Integer.parseInt(req.headerString(HttpHeaders.Content$2dLength));
      final Impl prev = this;
      if (cursor.remaining() != remaining) key.attach(new Impl() {
        @Override
        public void onRead(SelectionKey key) throws Exception {
          int read1 = channel.read(cursor);
          if (read1 == -1)
            key.cancel();
          if (!cursor.hasRemaining()) {
            key.interestOps(SelectionKey.OP_WRITE).attach(prev);
            return;
          }
        }

      });*/
    key.interestOps(SelectionKey.OP_WRITE);
  }


  public void onWrite(SelectionKey key) throws Exception {

    String accepts = req.headerString(Accept$2dEncoding);
    String ceString = null;
    String finalFname = scrub(rootPath + "/./" + req.path());
    file = new File(finalFname);
    if (null != accepts) {
//              String accepts = UTF8.decode((ByteBuffer) addHeaderInterest.clear().limit(ints[1]).position(ints[0])).toString().trim();
      for (CompressionTypes compType : CompressionTypes.values()) {
        if (accepts.contains(compType.name())) {
          File f = new File(finalFname + "." + compType.suffix);
          if (f.isFile() && f.canRead()) {

            if (BlobAntiPatternObject.DEBUG_SENDJSON)
              System.err.println("sending compressed archive: " + f.getAbsolutePath());
            ceString = (compType.name());
            file = f;
            break;
          }
        }
      }
    }
    boolean send200 = false;
    try {
      send200 = file.canRead() && file.isFile();
    } finally {

    }

    if (send200) {
      final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
      final long total = randomAccessFile.length();
      final FileChannel fileChannel = randomAccessFile.getChannel();


      String substring = finalFname.substring(finalFname.lastIndexOf('.') + 1);
      MimeType mimeType = MimeType.valueOf(substring);
      long length = randomAccessFile.length();

      final HttpResponse responseHeader = new Rfc822HeaderState().$res();

      responseHeader
          .status(HttpStatus.$200)
          .headerString(Content$2dType, (null == mimeType ? MimeType.bin : mimeType).contentType)
          .headerString(Content$2dLength, String.valueOf(length))
      ;
      if (null != ceString)
        responseHeader.headerString(Content$2dEncoding, ceString);
      ByteBuffer response = (ByteBuffer) responseHeader.as(ByteBuffer.class);
      int write = channel.write(response);
      final int sendBufferSize = BlobAntiPatternObject.getSendBufferSize();
      final long[] progress = {fileChannel.transferTo(0, sendBufferSize, channel)};
      key.interestOps(OP_WRITE | OP_CONNECT);
      key.selector().wakeup();
      key.attach(new Impl() {

        public void onWrite(SelectionKey key) throws Exception {
          long remaining = total - progress[0];
          progress[0] += fileChannel.transferTo(progress[0], min(sendBufferSize, remaining), channel);
          remaining = total - progress[0];
          if (0 == remaining) {
            fileChannel.close();
            randomAccessFile.close();
            key.selector().wakeup();
            key.interestOps(OP_READ);
            key.attach(null);
          }
        }
      });
    } else {
      key.selector().wakeup();
      key.interestOps(OP_WRITE).attach(new Impl() {


        public void onWrite(SelectionKey key) throws Exception {

          String response = "HTTP/1.1 404 Not Found\n" +
              "Content-Length: 0\n\n";
          System.err.println("!!! " + file.getAbsolutePath());
          int write = channel.write(UTF8.encode(response));
          key.selector().wakeup();
          key.interestOps(OP_READ).attach(null);
        }
      });
    }
  }
}
