package rxf.server;

import com.google.gson.annotations.SerializedName;

public class Attachment {

  private long length;

  @SerializedName("content_type")
  private String contentType;

  private boolean stub = true;

  public long getLength() {
    return length;
  }

  public String getContentType() {
    return contentType;
  }

}
