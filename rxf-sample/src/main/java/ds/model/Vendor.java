package ds.model;

import java.util.Date;

import com.google.gson.annotations.SerializedName;
import rxf.Seo;

public class Vendor {


  @SerializedName("_id")
  private String id;
  @Seo
  private
  String name;
  @Seo
  private
  String description;
  private String pocName;
  @Seo
  private
  Contact contactInfo;
  private Date creation;

  @SerializedName("_rev")
  private String version;


  public String getId() {
    return id;
  }


  public void setId(String id) {
    this.id = id;
  }


  public Date getCreation() {
    return creation;
  }


  public void setCreation(Date creation) {
    this.creation = creation;
  }


  public String getVersion() {
    return version;
  }


  public void setVersion(String version) {
    this.version = version;
  }


  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public String getDescription() {
    return description;
  }


  public void setDescription(String description) {
    this.description = description;
  }


  public String getPocName() {
    return pocName;
  }


  public void setPocName(String pocName) {
    this.pocName = pocName;
  }


  public Contact getContactInfo() {
    return contactInfo;
  }


  public void setContactInfo(Contact contactInfo) {
    this.contactInfo = contactInfo;
  }

}
