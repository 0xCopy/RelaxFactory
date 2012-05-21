package rxf.server;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 11:32 PM
 */
public class ApplicationContext {

  public <T> T getBean(Class<T> clazz) {
    T ret = null;
    try {
      System.err.println("class called is " + clazz.getCanonicalName());
      if (clazz == Visitor.class) {
        ret = (T) Visitor.createLocator().create(Visitor.class);
      } else {
        ret = clazz.newInstance();  //To change body of created methods use File | Settings | File Templates.
      }
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return ret;
  }
}
