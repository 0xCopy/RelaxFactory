package rxf.server;

//import rxf.server.rf.SessionFindLocatorVisitor;

/**
 * User: jim
 * Date: 4/16/12
 * Time: 1:22 PM
 */
public class VisitorLocator extends CouchLocator <Visitor>{
  @Override
  public Class<Visitor> getDomainType() {
    return Visitor.class;  //todo: verify for a purpose
  }

  @Override
  public String getId(Visitor domainObject) {
    return domainObject.getId();  //todo: verify for a purpose
  }

  @Override
  public Object getVersion(Visitor domainObject) {
    return domainObject.getVersion();  //todo: verify for a purpose
  }
}
