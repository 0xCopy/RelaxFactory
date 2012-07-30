package rxf.server.an;

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
@Target({FIELD, LOCAL_VARIABLE, TYPE, ANNOTATION_TYPE, CONSTRUCTOR, PACKAGE,
		PARAMETER})
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
			public <T> boolean validate(T... data) {
				final String t = (String) data[0];
				return t.toString().length() > 0 && !t.startsWith("\"")
						&& !t.endsWith("\"");
			}
		},
		designDocId, view, validjson, mimetype, mimetypeEnum {
			{
				clazz = MimeType.class;
			}
		},
		blob {
			{
				clazz = ByteBuffer.class;
			}
		},
		type {
			{
				clazz = Class.class;
			}
		};

		public <T> boolean validate(T... data) {
			return true;
		}

		public Class clazz = String.class;
	}

	etype[] value();

	etype[] optional() default {};
}
