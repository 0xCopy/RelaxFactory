package rxf.server;

import java.lang.annotation.*;
import java.nio.ByteBuffer;

import one.xio.MimeType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, LOCAL_VARIABLE, TYPE, ANNOTATION_TYPE, CONSTRUCTOR, PACKAGE, PARAMETER})
public @interface DbKeys {
  enum etype {

    opaque, db, docId, rev {
      /**
       * couchdb only returns a quoted etag for entities.  this quoted etag breaks in queries sent back to couchdb as rev="breakage"
       * @param data
       * @param <T>
       * @return
       */
      @Override
      <T> boolean validate(T... data) {
        final String t = (String) data[0];
        return t.toString().length() > 0 && !t.startsWith("\"") && !t.endsWith("\"");
      }
    }, designDocId, view, validjson, mimetype ,mimetypeHardcore {{
      clazz = MimeType.class;
    }}, blob {{
      clazz = ByteBuffer.class;
    }}, type {{
      clazz = Class.class;
    }};

    <T> boolean validate(T... data) {
      return true;
    }

    Class clazz = String.class;
  }


  etype[] value();
  etype[] optional() default {};
//
//
//  public static abstract class ReturnAction<T> {
//
//    static ThreadLocal<ReturnAction> currentResults = new ThreadLocal<ReturnAction>();
//
//    public ReturnAction() {
//      currentResults.set(this);
//    }
//  }
//

}

