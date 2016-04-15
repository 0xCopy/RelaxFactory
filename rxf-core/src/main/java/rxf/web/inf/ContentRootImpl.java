package rxf.web.inf;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpStatus;
import one.xio.MimeType;
import rxf.core.CouchNamespace;
import rxf.core.DateHeaderParser;
import rxf.core.Errors;
import rxf.core.Rfc822HeaderState.HttpRequest;
import rxf.core.Rfc822HeaderState.HttpResponse;
import rxf.core.Tx;
import rxf.shared.CompressionTypes;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.SelectionKey;
import java.nio.file.Paths;
import java.util.regex.MatchResult;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.AsioVisitor.Helper.finishWrite;
import static one.xio.AsioVisitor.Helper.write;
import static one.xio.HttpHeaders.*;

/**
 * deprecated -- Tx.fetchHttpHeaders and Tx.finishRequest change things User: jim Date: 6/4/12 Time: 1:42 AM
 */
@OpInterest(value = SelectionKey.OP_WRITE)
@Deprecated
public class ContentRootImpl extends Impl {

  public static final String SLASHDOTSLASH = File.separator + "." + File.separator;
  public static final String DOUBLESEP = File.separator + File.separator;
  private static final boolean DEBUG_SENDJSON = false;
  private ByteBuffer cursor;
  private MatchResult matchResults;
  /**
   * threadlocal from creation-time
   */
  Tx tx = Tx.current();
  public static final boolean USE_INVERSE_CHAR = '/' == File.separatorChar;
  public static final char INVERSE_CHAR = USE_INVERSE_CHAR ? '\\' : '/';

  public ContentRootImpl() {
    tx.state(tx.hdr().addHeaderInterest(Accept$2dEncoding, If$2dModified$2dSince,
        If$2dUnmodified$2dSince).read((ByteBuffer) tx.hdr().headerBuf().rewind()).asRequest());
    assert null != tx.payload() : "Tx.current() returns null, required non-null by ContentRootImpl";
  }

  public static String fileScrub(String scrubMe) {
    char inverseChar = '/' == File.separatorChar ? '\\' : '/';
    return null == scrubMe ? null : Paths.get(scrubMe.trim()).normalize()//
        .toString()//
        .replace(inverseChar, File.separatorChar)//
        .replace(DOUBLESEP, "" + File.separator)//
        .replace("..", ".");//
  }

  public void onWrite(SelectionKey key) throws Exception {

    String finalFname = fileScrub(getRootPath() + SLASHDOTSLASH + getReq().path().split("\\?")[0]);
    File file = new File(finalFname);
    if (file.isDirectory()) {
      file = new File((finalFname + "/index.html"));
    }
    finalFname = (file.getCanonicalPath());
    System.err.println("ContentRootImpl write entered: " + finalFname);

    java.util.Date fdate = new java.util.Date(file.lastModified());

    String since = getReq().headerString(If$2dModified$2dSince);
    String accepts = getReq().headerString(Accept$2dEncoding);

    final HttpResponse res = getReq().$res();
    if (null != since) {
      java.util.Date cachedDate = DateHeaderParser.parseDate(since);

      if (cachedDate.after(fdate)) {

        res.status(HttpStatus.$304).headerString(Connection, "close").headerString(Last$2dModified,
            DateHeaderParser.formatHttpHeaderDate(fdate));
        int write = write(key, res.asByteBuffer());
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
          int write = write(key, res.asByteBuffer());
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
          Errors.$404(key, res.asRequest().path());
          key.selector().wakeup();
          key.interestOps(OP_READ).attach(null);
        }
      });
    }
  }

  public void sendFile(final SelectionKey key, String finalFname, File file, java.util.Date fdate,
      HttpResponse res, String ceString) throws IOException {
    final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
    String substring = finalFname.substring(finalFname.lastIndexOf('.') + 1);
    MimeType mimeType = MimeType.valueOf(substring);
    long length = randomAccessFile.length();

    res.status(HttpStatus.$200).headerString(Content$2dType,
        ((null == mimeType) ? MimeType.bin : mimeType).contentType).headerString(Content$2dLength,
        String.valueOf(length)).headerString(Connection, "close").headerString(Date,
        DateHeaderParser.formatHttpHeaderDate(fdate));
    if (null != ceString)
      res.headerString(Content$2dEncoding, ceString);

    try {
      MappedByteBuffer map = randomAccessFile.getChannel().map(MapMode.READ_ONLY, 0, length);
      Buffer fileContent = map.rewind();
      Buffer headers = res.asByteBuffer().rewind();
      finishWrite(key, new Runnable() {
        @Override
        public void run() {
          try {
            randomAccessFile.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
          try {
            key.channel().close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }, (ByteBuffer) headers, (ByteBuffer) fileContent);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ByteBuffer getCursor() {
    return cursor;
  }

  public void setCursor(ByteBuffer cursor) {
    this.cursor = cursor;
  }

  public HttpRequest getReq() {
    return tx.hdr().asRequest();
  }

  public void setReq(HttpRequest req) {
    tx.state(req);
  }

  public String getRootPath() {
    return CouchNamespace.RXF_CONTENT_ROOT;
  }

  public void setRootPath(String rootPath) {
    throw new AbstractMethodError();
  }

  public void setMatchResults(MatchResult matchResults) {
    this.matchResults = matchResults;
  }

  /**
   * todo: make cleaner. make this go away
   * 
   * @param key
   * @return
   */
  public MatchResult getMatchResults(SelectionKey key) {
    if (matchResults == null) {
      Object attachment = key.attachment();
      if (attachment instanceof Object[]) {
        Object[] objects = (Object[]) attachment;
        for (Object object : objects) {
          if (object instanceof MatchResult) {
            MatchResult matchResult1 = (MatchResult) object;
            return matchResults = matchResult1;
          }
        }
      }

    }
    return matchResults;
  }
}
