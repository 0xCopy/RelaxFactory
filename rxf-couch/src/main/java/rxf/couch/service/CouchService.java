package rxf.couch.service;

import com.google.gson.JsonObject;
import one.xio.HttpHeaders;
import rxf.couch.driver.Attachments;
import rxf.couch.gen.CouchDriver.DocDelete;
import rxf.couch.gen.CouchDriver.DocFetch;
import rxf.couch.gen.CouchDriver.JsonSend;
import rxf.couch.gen.CouchDriver.JsonSend.JsonSendActionBuilder;
import rxf.shared.CouchTx;
import rxf.shared.KouchTx;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

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

}
