package rxf.server;


/**
 * User: jim
 * Date: 4/16/12
 * Time: 1:22 PM
 */
public class VisitorLocator extends CouchLocator <Visitor>{
  @Override
  public Class<Visitor> getDomainType() {
    return Visitor.class;
  }

  @Override
  public String getId(Visitor domainObject) {
    return domainObject.getId();
  }

  @Override
  public Object getVersion(Visitor domainObject) {
    return domainObject.getVersion();
  }
}
