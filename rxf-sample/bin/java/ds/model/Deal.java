package ds.model;

import java.util.Date;

import com.google.gson.annotations.SerializedName;
import ds.server.SecurityImpl;
import rxf.Seo;


/**
 * <ul>
 * <li> Login proposes a deal with product, discount, vendor, and npo.
 * <ul>
 * <li> expiration is set.
 * </ul>
 * <li> Vendor proposes a modified deal with a cap/amount.
 * <ul>
 * <li>expiration is reset
 * <li> Login is pre-accepted and emailed a link to the reservation page.
 * <li> other Visitors view and reserve up to minimum
 * <li> reserved transactions are completed.
 * <li> deal proceeds up to limit
 * </ul>
 * <li>-or- Deal expiration date passes.
 * <ul>
 * <li> reserved transactions are cancelled at payment gateway (typically at no charge)
 * <li>email notifies Visitors and Vendors or expiration and automated reservation cancelation gateway
 * timeframesfrom minutes to 24 hours to release
 * </li>
 * <p/>
 * </li>
 * </ul>
 * User: jim
 * Date: 5/11/12
 * Time: 4:09 PM
 */
public class Deal /*implements DealProxy*/ {


  @SerializedName("_id")
  private String id;

  private Date creation;

  @SerializedName("_rev")
  private String version;

  @Seo()
  private String product;
  @Seo()
  public String productDescription;

  private transient Login creator;
  private String creatorId;
  transient private Npo npo;
  private String npoId;
  transient private Vendor vendor;
  private String vendorId;
  private String roAdmin;
  private Float discount;
  private Float amount;

  private Integer minimum;
  private Integer limit;
  private Date expire;


  private String json;


  public String getId() {
    return id;
  }


  public void setId(String id) {
    this.id = id;
  }


  public Date getCreation() {
    return creation;
  }

  @Override
  public String toString() {
    return "Deal{" +
        "id='" + id + '\'' +
        ", creation=" + creation +
        ", version='" + version + '\'' +
        ", product='" + product + '\'' +
        ", productDescription='" + productDescription + '\'' +
        ", creator=" + creator +
        ", creatorId='" + creatorId + '\'' +
        ", npo=" + npo +
        ", npoId='" + npoId + '\'' +
        ", vendor=" + vendor +
        ", vendorId='" + vendorId + '\'' +
        ", roAdmin='" + roAdmin + '\'' +
        ", discount=" + discount +
        ", amount=" + amount +
        ", minimum=" + minimum +
        ", limit=" + limit +
        ", expire=" + expire +
        ", json='" + json + '\'' +
        '}';
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


  public String getProduct() {
    return product;
  }


  public void setProduct(String product) {
    this.product = product;
  }


  public String getProductDescription() {
    return productDescription;
  }


  public void setProductDescription(String productDescription) {
    this.productDescription = productDescription;
  }


  public Login getCreator() {
    return creator == null ? creator = SecurityImpl.LOGIN_COUCH_LOCATOR.find(Login.class, getId()) : creator;
  }


  public String getCreatorId() {
    return creatorId;
  }


  public void setCreatorId(String creatorId) {
    setCreator(null);
    this.creatorId = creatorId;
  }


  public void setCreator(Login creator) {
    this.creator = creator;
    setCreatorId(creator.getId());
  }


  public Vendor getVendor() {
    return vendor == null && getVendorId() != null ? vendor = SecurityImpl.VENDOR_COUCH_LOCATOR.find(Vendor.class, getVendorId()) : vendor;

  }


  public String getVendorId() {
    return vendorId;
  }


  public void setVendorId(String vendorId) {
    this.vendor = null;
    this.vendorId = vendorId;
  }


  public void setVendor(Vendor vendor) {
    //first, reset the id
    if (vendor == null) {
      setVendorId(null);
    } else {
      if (vendor.getId() == null) {
        throw new IllegalArgumentException("Vendor isn't null, but it has no ID, can't link to it.");
      }
      setVendorId(vendor.getId());
    }
    //then, since setting the id clears the local reference, set it again
    this.vendor = vendor;
  }


  public Npo getNpo() {
    return npo == null && getNpoId() != null ? npo = SecurityImpl.NPO_COUCH_LOCATOR.find(Npo.class, getNpoId()) : npo;

  }


  public String getNpoId() {
    return npoId;
  }


  public void setNpoId(String npoId) {
    this.npo = null;
    this.npoId = npoId;
  }


  public void setNpo(Npo npo) {
    //first, reset the id
    if (npo == null) {
      setNpoId(null);
    } else {
      if (npo.getId() == null) {
        throw new IllegalArgumentException("Npo isn't null, but it has no ID, can't link to it");
      }
      setNpoId(npo.getId());
    }
    this.npo = npo;
  }


  public String getRoAdmin() {
    return roAdmin;
  }


  public void setRoAdmin(String roAdmin) {
    this.roAdmin = roAdmin;
  }

  /**
   * misc workflow state
   */
  /**
   * catch-all for workflow state -- confirmation ids,
   *
   * @return
   */


  public String getJson() {
    return json;
  }


  public void setJson(String json) {
    this.json = json;
  }

  /**
   * set by creator, adjusted by vendor
   *
   * @return
   */


  public Float getDiscount() {
    return discount;
  }


  public void setDiscount(Float discount) {
    this.discount = discount;
  }

  /**
   * set by vendor
   *
   * @return
   */


  public Float getAmount() {
    return amount;
  }


  public void setAmount(Float amount) {
    this.amount = amount;
  }

  /**
   * next workflow goal may expire at this point in time
   */
  /**
   * workflow sweeps up old stuff where this is set and puts operational data to pasture
   *
   * @return
   */


  public Date getExpire() {
    return expire;
  }


  public void setExpire(Date expire) {
    this.expire = expire;
  }

  /**
   * number of commitment
   *
   * @return
   */


  public Integer getMinimum() {
    return minimum;
  }


  public void setMinimum(Integer minimum) {
    this.minimum = minimum;
  }

  /**
   * max deals
   *
   * @return
   */


  public Integer getLimit() {
    return limit;
  }


  public void setLimit(Integer limit) {
    this.limit = limit;
  }

}

