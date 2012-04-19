package ro.server;

import javax.servlet.ServletContext;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 11:32 PM
 */
public class WebApplicationContextUtils {
  public static ApplicationContext getWebApplicationContext(ServletContext servletContext) {
    return new ApplicationContext();  //To change body of created methods use File | Settings | File Templates.
  }
}
