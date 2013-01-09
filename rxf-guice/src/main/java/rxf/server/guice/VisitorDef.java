package rxf.server.guice;

import com.google.inject.Key;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;

public class VisitorDef {
  private final HttpMethod method;
  private final String pattern;
  private final Key<? extends AsioVisitor> visitorKey;

  public VisitorDef(HttpMethod method, String pattern, Key<? extends AsioVisitor> visitor) {
    this.method = method;
    this.pattern = pattern;
    this.visitorKey = visitor;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public String getPattern() {
    return pattern;
  }

  public Key<? extends AsioVisitor> getVisitorKey() {
    return visitorKey;
  }

}
