package rxf.web.inf;

import one.xio.HttpStatus;
import rxf.core.Errors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.channels.SelectionKey;

/**
 * Created by jim on 6/13/14.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecureScope {

  String AUTH = "/auth";
  String SESSION = "session";
  String SSID = "SSID";

  String value() default SSID;

  String authPath() default AUTH;

  String authDb() default SESSION;

  @SecureScope
  class ContentRootCacheImpl extends rxf.web.inf.ContentRootCacheImpl {
  }

  @SecureScope
  class ContentRootNoCacheImpl extends rxf.web.inf.ContentRootNoCacheImpl {
  }

  @SecureScope
  class ContentRootImpl extends rxf.web.inf.ContentRootImpl {
  }

  class Forbidden extends rxf.web.inf.ContentRootImpl {
    @Override
    public void onWrite(SelectionKey key) throws Exception {
      // todo: log attempted theft.
      Errors.$404(key, HttpStatus.$404.caption);
    }
  }
}
