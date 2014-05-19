package rxf.web.inf;

import one.xio.HttpHeaders;
import rxf.core.DateHeaderParser;

import java.nio.channels.SelectionKey;
import java.util.Date;
import java.util.regex.Pattern;

public class ContentRootNoCacheImpl extends ContentRootImpl {

  public static final Pattern NOCACHE_PATTERN = Pattern.compile(".*[.]nocache[.](js|html)$");

  @Override
  public void onWrite(SelectionKey key) throws Exception {
    req.headerStrings().put(HttpHeaders.Expires.getHeader(),
        DateHeaderParser.RFC1123.getFormat().format(new Date()));
    super.onWrite(key);
  }
}
