package rxf.couch;

import com.google.gson.JsonObject;
import one.xio.HttpHeaders;
import rxf.couch.driver.CouchMetaDriver;
import rxf.couch.gen.CouchDriver.DocDelete;
import rxf.couch.gen.CouchDriver.DocFetch;
import rxf.couch.gen.CouchDriver.JsonSend;
import rxf.couch.gen.CouchDriver.JsonSend.JsonSendActionBuilder;
import rxf.shared.CouchTx;
import rxf.shared.KouchTx;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.*;

/**
 * Declares a generated service that can be implemented automatically by CouchServiceFactory. Specific to a single
 * entity backed by CouchDB, E. Comes with two built-in methods: to find an object by key, and to persist a given
 * object.
 * <p/>
 * The views will all be created in a design document in the specific database for E.
 * 
 * @param <E> the entity this service is concerned with.
 */
public interface CouchService<E> {
  E find(String key);

  CouchTx persist(E entity);

  Attachments attachments(E entity);

  public static interface Attachments {
    CouchTx addAttachment(String content, String filename, String contentType);

    Writer addAttachment(String fileName, String contentType);

    CouchTx updateAttachment(String content, String fileName, String contentType);

    Writer updateAttachment(String fileName, String contentType);

    String getAttachment(String fileName);

    KouchTx deleteAttachment(String fileName);

  }

  static class AttachmentsImpl<E> implements Attachments {
    private final E entity;
    private String rev;
    private String id;
    private String db;

    public AttachmentsImpl(String db, E entity) throws NoSuchFieldException, IllegalAccessException {
      this.entity = entity;
      JsonObject obj = CouchMetaDriver.gson().toJsonTree(entity).getAsJsonObject();
      rev = obj.get("_rev").getAsString();
      id = obj.get("_id").getAsString();
      this.db = db;
    }

    public CouchTx addAttachment(String content, String fileName, String contentType) {
      JsonSendActionBuilder actionBuilder =
          new JsonSend().opaque(db + "/" + id + "/" + fileName + "?rev=" + rev).validjson(content)
              .to();
      actionBuilder.state().headerString(HttpHeaders.Content$2dType, contentType);
      CouchTx tx = actionBuilder.fire().tx();
      rev = tx.rev();
      return tx;
    }

    public Writer addAttachment(final String fileName, final String contentType) {
      return new StringWriter() {

        public void close() throws IOException {
          JsonSendActionBuilder actionBuilder =
              new JsonSend().opaque(db + "/" + id + "/" + fileName + "?rev=" + rev).validjson(
                  getBuffer().toString()).to();
          actionBuilder.state().headerString(HttpHeaders.Content$2dType, contentType);
          CouchTx tx = actionBuilder.fire().tx();
          if (!tx.ok()) {
            throw new IOException(tx.error());
          }
          rev = tx.rev();
        }
      };
    }

    public CouchTx updateAttachment(String content, String fileName, String contentType) {
      JsonSendActionBuilder actionBuilder =
          new JsonSend().opaque(db + "/" + id + "/" + fileName + "?rev=" + rev).validjson(content)
              .to();
      actionBuilder.state().headerString(HttpHeaders.Content$2dType, contentType);
      CouchTx tx = actionBuilder.fire().tx();
      rev = tx.rev();
      return tx;
    }

    public Writer updateAttachment(final String fileName, final String contentType) {
      return new StringWriter() {

        public void close() throws IOException {
          JsonSendActionBuilder actionBuilder =
              new JsonSend().opaque(db + "/" + id + "/" + fileName + "?rev=" + rev).validjson(
                  getBuffer().toString()).to();
          actionBuilder.state().headerString(HttpHeaders.Content$2dType, contentType);
          CouchTx tx = actionBuilder.fire().tx();
          if (!tx.ok()) {
            throw new IOException(tx.error());
          }
          rev = tx.rev();
        }
      };
    }

    public String getAttachment(String fileName) {
      return new DocFetch().db(db).docId(id + "/" + fileName).to().fire().json();
    }

    public KouchTx deleteAttachment(String fileName) {
      CouchTx tx = new DocDelete().db(db).docId(id + "/" + fileName).to().fire().tx();
      rev = tx.rev();
      return tx;
    }
  }

  /**
   * Describes the JavaScript view to run in CouchDB when this method is invoked. The map function is required, but the
   * reduce function is optional.
   * <p/>
   * Methods decorated with this should return List or CouchResultSet
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Documented
  public @interface View {
    String map();

    /**
     * Typically empty (the default, meaning "don't reduce"), "_sum", "_count", or "_stats".
     */
    String reduce() default "";
  }

  /**
   * Indicates that the decorated annotation is a couch request param.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.ANNOTATION_TYPE)
  public @interface CouchRequestParam {
    /**
     * The parameter name to use in the request
     */
    String value();

    /**
     * True if the value should be encoded as json. If false, the parameter value will be toString'd to send it over the
     * wire. Defaults to true.
     */
    boolean isJson() default true;
  }

  /**
   * Marks a service call parameter as being used per a couchdb view GET request as "key".
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @CouchRequestParam("key")
  @Documented
  public @interface Key {
  }

  /**
   * Marks a service method or parameter as being used per a couchdb view GET request as "keys". Typically used per a
   * parameter that accepts an array or collection of keys.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @CouchRequestParam("keys")
  @Documented
  public @interface Keys {
  }

  /**
   * Marks a service method or parameter as being used per a couchdb view GET request as "limit". A value need not be
   * provided if used per a parameter, but the annotation is useless per the method without a value.
   * <p/>
   * (Not yet supported per a method)
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD})
  @CouchRequestParam("limit")
  @Documented
  public @interface Limit {
    /**
     * Value to use as the "limit" parameter. Annotations per the method override possible parameters.
     */
    int value() default -1;
  }

  /**
   * Marks a service method or parameter as being used per a couchdb view GET request as "skip". A value need not be
   * provided if used per a parameter, but the annotation is useless per the method without a value.
   * <p/>
   * From http://wiki.apache.org/couchdb/HTTP_view_API#Querying_Options: <blockquote>"The skip option should only be
   * used with small values, as skipping a large range of documents this way is inefficient (it scans the index from the
   * startkey and then skips N elements, but still needs to read all the index values to do that). For efficient paging
   * you'll need to use startkey and limit. If you expect to have multiple documents emit identical keys, you'll need to
   * use startkey_docid in addition to startkey to paginate correctly. The reason is that startkey alone will no longer
   * be sufficient to uniquely identify a row."</blockquote>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD})
  @CouchRequestParam("skip")
  @Documented
  public @interface Skip {
    /**
     * Value to use as the "skip" parameter. Annotations per the method override possible parameters.
     */
    int value() default -1;
  }

  /**
   * Marks a service method or parameter as being used per a couchdb view GET request as "startkey". In conjunction with
   * endkey and/or limit can be used to implement basic prefix search
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @CouchRequestParam("startkey")
  @Documented
  public @interface StartKey {
  }

  /**
   * Marks a service method or parameter as being used per a couchdb view GET request as "endkey".
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @CouchRequestParam("endkey")
  @Documented
  public @interface EndKey {
  }

  /**
   * Marks a service method or parameter as being used per a couchdb view GET request as "startkey_docid". Allows for
   * pagination by id in case of duplicate 'startkey', and without the performance issues of 'skip'.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @CouchRequestParam("startkey_docid")
  @Documented
  public @interface StartKeyDocId {
  }

  /**
   * Marks a service method or parameter as being used per a couchdb view GET request as "endkey_docid". Allows for
   * pagination by id in case of duplicate 'endkey'.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @CouchRequestParam("endkey_docid")
  @Documented
  public @interface EndKeyDocId {
  }

  /**
   * Changes the direction of a search.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD})
  @CouchRequestParam("descending")
  @Documented
  public @interface Descending {
    boolean value() default false;
  }

  /**
   * The group option controls whether the reduce function reduces to a set of distinct keys or to a single result row.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD})
  @CouchRequestParam("group")
  @Documented
  public @interface Group {
    boolean value() default false;
  }

  /**
   * The group_level argument. See http://wiki.apache.org/couchdb/HTTP_view_API#Querying_Options for additional details.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD})
  @CouchRequestParam("group")
  @Documented
  public @interface GroupLevel {
    int value() default 0;
  }

}
