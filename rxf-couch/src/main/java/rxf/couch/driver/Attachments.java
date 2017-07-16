package rxf.couch.driver;

import rxf.shared.CouchTx;
import rxf.shared.KouchTx;

import java.io.Writer;

public interface Attachments {
  CouchTx addAttachment(String content, String filename, String contentType);

  Writer addAttachment(String fileName, String contentType);

  CouchTx updateAttachment(String content, String fileName, String contentType);

  Writer updateAttachment(String fileName, String contentType);

  String getAttachment(String fileName);

  KouchTx deleteAttachment(String fileName);

}
