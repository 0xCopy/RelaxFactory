package rxf.web.inf;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpStatus;
import one.xio.MimeType;
import rxf.core.CouchNamespace;
import rxf.core.DateHeaderParser;
import rxf.core.Rfc822HeaderState;
import rxf.core.Rfc822HeaderState.HttpRequest;
import rxf.core.Rfc822HeaderState.HttpResponse;
import rxf.shared.CompressionTypes;
import rxf.shared.PreRead;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.regex.MatchResult;

import static java.lang.Math.min;
import static java.nio.channels.SelectionKey.*;
import static one.xio.HttpHeaders.*;

/**
 * User: jim Date: 6/4/12 Time: 1:42 AM
 */
@PreRead
public class ContentRootImpl extends Impl implements ServiceHandoff {

  public static final String SLASHDOTSLASH = File.separator + "." + File.separator;
  public static final String DOUBLESEP = File.separator + File.separator;
  private static final boolean DEBUG_SENDJSON = false;
  private String rootPath = CouchNamespace.COUCH_DEFAULT_FS_ROOT;
  private ByteBuffer cursor;
  private SocketChannel channel;
  private HttpRequest req;
  private MatchResult matchResults;

  public static String fileScrub(String scrubMe) {
    char inverseChar = '/' == File.separatorChar ? '\\' : '/';
    return null == scrubMe ? null : scrubMe.trim().replace(inverseChar, File.separatorChar)
        .replace(DOUBLESEP, "" + File.separator).replace("..", ".");
  }

  @Override
  public void onRead(SelectionKey key) throws Exception {
    setChannel((SocketChannel) key.channel());
    if (getCursor() == null) {
      if (key.attachment() instanceof Object[]) {
        Object[] ar = (Object[]) key.attachment();
        for (Object o : ar) {
          if (o instanceof ByteBuffer) {
            setCursor((ByteBuffer) o);
            continue;
          }
          if (o instanceof Rfc822HeaderState) {
            setReq(((Rfc822HeaderState) o).$req());
            continue;
          }
          if (o instanceof MatchResult) {
            setMatchResults((MatchResult) o);
            continue;
          }
        }
      }
      key.attach(this);
    }
    setCursor(null == getCursor() ? ByteBuffer.allocateDirect(4 << 10) : getCursor().hasRemaining()
        ? getCursor() : ByteBuffer.allocateDirect(getCursor().capacity() << 1).put(
            (ByteBuffer) getCursor().rewind()));
    int read = getChannel().read(getCursor());
    if (read == -1)
      key.cancel();
    Buffer flip = getCursor().duplicate().flip();

    setReq((HttpRequest) new Rfc822HeaderState().addHeaderInterest(Accept$2dEncoding,
        If$2dModified$2dSince, If$2dUnmodified$2dSince).$req().read((ByteBuffer) flip));
    if (!Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR, getReq()
        .headerBuf())) {
      return;
    }
    setCursor(((ByteBuffer) flip).slice());
    key.interestOps(SelectionKey.OP_WRITE);
  }

  public void onWrite(SelectionKey key) throws Exception {

    String finalFname = fileScrub(getRootPath() + SLASHDOTSLASH + getReq().path().split("\\?")[0]);
    File file = new File(finalFname);
    if (file.isDirectory()) {
      file = new File((finalFname + "/index.html"));
    }
    finalFname = (file.getCanonicalPath());

    java.util.Date fdate = new java.util.Date(file.lastModified());

    String since = getReq().headerString(If$2dModified$2dSince);
    String accepts = getReq().headerString(Accept$2dEncoding);

    HttpResponse res = getReq().$res();
    if (null != since) {
      java.util.Date cachedDate = DateHeaderParser.parseDate(since);

      if (cachedDate.after(fdate)) {

        res.status(HttpStatus.$304).headerString(Connection, "close").headerString(Last$2dModified,
            DateHeaderParser.formatHttpHeaderDate(fdate));
        int write = getChannel().write(res.as(ByteBuffer.class));
        key.interestOps(OP_READ).attach(null);
        return;
      }
    } else {
      since = getReq().headerString(If$2dUnmodified$2dSince);

      if (null != since) {
        java.util.Date cachedDate = DateHeaderParser.parseDate(since);

        if (cachedDate.before(fdate)) {

          res.status(HttpStatus.$412).headerString(Connection, "close").headerString(
              Last$2dModified, DateHeaderParser.formatHttpHeaderDate(fdate));
          int write = getChannel().write(res.as(ByteBuffer.class));
          key.interestOps(OP_READ).attach(null);
          return;
        }
      }
    }
    String ceString = null;
    if (null != accepts) {

      for (CompressionTypes compType : CompressionTypes.values()) {
        if (accepts.contains(compType.name())) {
          File f = new File(file.getAbsoluteFile() + "." + compType.suffix);
          if (f.isFile() && f.canRead()) {
            if (DEBUG_SENDJSON) {
              System.err.println("sending compressed archive: " + f.getAbsolutePath());
            }
            ceString = (compType.name());
            file = f;
            break;
          }
        }
      }
    }
    boolean send200 = file.canRead() && file.isFile();

    if (send200)
      sendFile(key, finalFname, file, fdate, res, ceString);
    else {
      key.selector().wakeup();
      key.interestOps(OP_WRITE).attach(new Impl() {

        public void onWrite(SelectionKey key) throws Exception {

          getChannel().write(
              getReq().$res().status(HttpStatus.$404).headerString(Content$2dLength, "0").as(
                  ByteBuffer.class));
          key.selector().wakeup();
          key.interestOps(OP_READ).attach(null);
        }
      });
    }
  }

  public void sendFile(SelectionKey key, String finalFname, File file, java.util.Date fdate,
      HttpResponse res, String ceString) throws IOException {
    final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
    final long total = randomAccessFile.length();
    final FileChannel fileChannel = randomAccessFile.getChannel();

    String substring = finalFname.substring(finalFname.lastIndexOf('.') + 1);
    MimeType mimeType = MimeType.valueOf(substring);
    long length = randomAccessFile.length();

    res.status(HttpStatus.$200).headerString(Content$2dType,
        ((null == mimeType) ? MimeType.bin : mimeType).contentType).headerString(Content$2dLength,
        String.valueOf(length)).headerString(Connection, "close").headerString(Date,
        DateHeaderParser.formatHttpHeaderDate(fdate));
    if (null != ceString)
      res.headerString(Content$2dEncoding, ceString);
    ByteBuffer response = res.as(ByteBuffer.class);
    getChannel().write(response);
    final int sendBufferSize = 4 << 10;
    final long[] progress = {fileChannel.transferTo(0, sendBufferSize, getChannel())};
    key.interestOps(OP_WRITE | OP_CONNECT);
    key.selector().wakeup();
    key.attach(new Impl() {

      public void onWrite(SelectionKey key) throws Exception {
        long remaining = total - progress[0];
        progress[0] +=
            fileChannel.transferTo(progress[0], min(sendBufferSize, remaining), getChannel());
        remaining = total - progress[0];
        if (0 == remaining) {
          fileChannel.close();
          randomAccessFile.close();
          key.selector().wakeup();
          key.interestOps(OP_READ).attach(null);
        }
      }
    });
  }

  @Override
  public ByteBuffer getCursor() {
    return cursor;
  }

  @Override
  public void setCursor(ByteBuffer cursor) {
    this.cursor = cursor;
  }

  @Override
  public SocketChannel getChannel() {
    return channel;
  }

  @Override
  public void setChannel(SocketChannel channel) {
    this.channel = channel;
  }

  @Override
  public HttpRequest getReq() {
    return req;
  }

  @Override
  public void setReq(HttpRequest req) {
    this.req = req;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public void setMatchResults(MatchResult matchResults) {
    this.matchResults = matchResults;
  }

  public MatchResult getMatchResults() {
    return matchResults;
  }
}
