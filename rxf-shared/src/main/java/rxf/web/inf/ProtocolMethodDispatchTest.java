package rxf.web.inf;

import org.junit.Assert;
import org.junit.Test;

import java.util.TreeSet;

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 1/31/13
 * Time: 10:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class ProtocolMethodDispatchTest {

  public static final String[] STRINGS =
      new String[] {
          "admin/288336E8BDD77AD2C73B3E1800E08FE8.cache.html",
          "admin/4721255C711C4B15FDDE7EA99202031B.gwt.rpc",
          "admin/6F9AE1C494399597F2BE0AF6C6554F4C.cache.html",
          "admin/7ABC097B81762AAD0A07A0D4A34AA196.cache.html",
          "admin/84064F4DC76F593373C541F910201444.cache.html",
          "admin/A4274D0486F8647E7D7AAA66D576A28A.cache.html",
          "admin/F2C4292978C936061BF84205286CCFBE.cache.html", "admin/admin.nocache.js",
          "admin/clear.cache.gif", "admin/gwt", "admin/gwt/standard", "admin/gwt/standard/images",
          "admin/gwt/standard/images/corner.png", "admin/gwt/standard/images/corner_ie6.png",
          "admin/gwt/standard/images/hborder.png", "admin/gwt/standard/images/hborder_ie6.png",
          "admin/gwt/standard/images/ie6",
          "admin/gwt/standard/images/ie6/corner_dialog_topleft.png",
          "admin/gwt/standard/images/ie6/corner_dialog_topright.png",
          "admin/gwt/standard/images/ie6/hborder_blue_shadow.png",
          "admin/gwt/standard/images/ie6/hborder_gray_shadow.png",
          "admin/gwt/standard/images/ie6/vborder_blue_shadow.png",
          "admin/gwt/standard/images/ie6/vborder_gray_shadow.png",
          "admin/gwt/standard/images/splitPanelThumb.png", "admin/gwt/standard/images/vborder.png",
          "admin/gwt/standard/images/vborder_ie6.png", "admin/gwt/standard/standard.css",
          "admin/gwt/standard/standard_rtl.css", "admin/hosted.html", "admin/oauthWindow.html",
          "DealSite", "DealSite/346AE8727F0CD871C8884446C90BF63D.cache.html",
          "DealSite/4721255C711C4B15FDDE7EA99202031B.gwt.rpc",
          "DealSite/7D3A17B0C939BC8FEA59EF397ABD3CCC.cache.html",
          "DealSite/8F9B42C6DD5F2060675A039943D9DCF7.cache.html",
          "DealSite/AE658E7DBF1ADF340941D86E5E011B58.cache.html",
          "DealSite/BC5DE76A55558DE34AA22DA390AE4C6E.cache.html",
          "DealSite/DCB3E63CDC1072EC08C308B253D42400.cache.html", "DealSite/DealSite.nocache.js",
          "DealSite/clear.cache.gif", "DealSite/gwt", "DealSite/gwt/standard",
          "DealSite/gwt/standard/images", "DealSite/gwt/standard/images/corner.png",
          "DealSite/gwt/standard/images/corner_ie6.png",
          "DealSite/gwt/standard/images/hborder.png",
          "DealSite/gwt/standard/images/hborder_ie6.png", "DealSite/gwt/standard/images/ie6",
          "DealSite/gwt/standard/images/ie6/corner_dialog_topleft.png",
          "DealSite/gwt/standard/images/ie6/corner_dialog_topright.png",
          "DealSite/gwt/standard/images/ie6/hborder_blue_shadow.png",
          "DealSite/gwt/standard/images/ie6/hborder_gray_shadow.png",
          "DealSite/gwt/standard/images/ie6/vborder_blue_shadow.png",
          "DealSite/gwt/standard/images/ie6/vborder_gray_shadow.png",
          "DealSite/gwt/standard/images/splitPanelThumb.png",
          "DealSite/gwt/standard/images/vborder.png",
          "DealSite/gwt/standard/images/vborder_ie6.png", "DealSite/gwt/standard/standard.css",
          "DealSite/gwt/standard/standard_rtl.css", "DealSite/hosted.html",
          "DealSite/oauthWindow.html", "META-INF", "Welcome.css", "Welcome.html", "admin.html",
          "index.html",};

  @Test
  public void testCachePatterns() {
    TreeSet<Integer> objects = new TreeSet<Integer>();
    for (int i = 0; i < STRINGS.length; i++) {
      String string = STRINGS[i];
      boolean b = ContentRootCacheImpl.CACHE_PATTERN.matcher(string).find();
      if (b)
        objects.add(i);

    }

    System.err.println(String.valueOf(objects));
    Assert.assertArrayEquals(objects.toArray(), new Integer[] {
        0, 2, 3, 4, 5, 6, 8, 31, 33, 34, 35, 36, 37, 39});

  }

  @Test
  public void testNocachePatterns() {
    TreeSet<Integer> objects = new TreeSet<Integer>();

    objects = new TreeSet<Integer>();
    for (int i = 0; i < STRINGS.length; i++) {
      String string = STRINGS[i];
      boolean b = ContentRootNoCacheImpl.NOCACHE_PATTERN.matcher(string).find();
      if (b)
        objects.add(i);

    }
    System.err.println(String.valueOf(objects));
    Assert.assertArrayEquals(objects.toArray(), new Integer[] {7, 38});
  }

}
