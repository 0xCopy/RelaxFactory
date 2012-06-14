
package rxf.server;
//generated
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * generated drivers
 */
public interface CouchDriver{rxf.server.CouchTx DbCreate( java.lang.String db, java.lang.String validjson );

 

public class DbCreate extends DbKeysBuilder<rxf.server.CouchTx> {
  private Rfc822HeaderState rfc822HeaderState;

  private DbCreate() {
  }

  static public DbCreate $() {
    return new DbCreate();
  }

  public interface DbCreateTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
     CouchTx tx();void oneWay();
  }

  public class DbCreateActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
    public DbCreateActionBuilder( /*<rxf.server.CouchTx>*/ ) {
      super(/*synchronousQueues*/);
    }

    @Override
    public DbCreateTerminalBuilder fire() {
      return new DbCreateTerminalBuilder() {
         public  CouchTx tx(){try {
        return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.DbCreate.visit();
      } catch (Exception e) {
        e.printStackTrace();   
      } return null;} public void oneWay(){
    final DbKeysBuilder<Object>dbKeysBuilder=(DbKeysBuilder<Object>)DbKeysBuilder.get();
 final ActionBuilder<Object>actionBuilder=(ActionBuilder<Object>)ActionBuilder.get();
dbKeysBuilder.validate();
 BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable(){
 
@Override
public void run(){
    try{

      DbKeysBuilder.currentKeys.set(dbKeysBuilder);   
      ActionBuilder.currentAction.set(actionBuilder); 
rxf.server.CouchMetaDriver.DbCreate.visit(/*dbKeysBuilder,actionBuilder*/);
}catch(Exception e){
    e.printStackTrace();}
    }
    });
}
      };
    }

    @Override
    public DbCreateActionBuilder state(Rfc822HeaderState state) {
      return (DbCreateActionBuilder) super.state(state);
    }

    @Override
    public DbCreateActionBuilder key(java.nio.channels.SelectionKey key) {
      return (DbCreateActionBuilder) super.key(key);
    }
  }

  @Override
  public DbCreateActionBuilder to( /*<rxf.server.CouchTx>*/ ) {
    if (parms.size() == parmsCount)
      return new DbCreateActionBuilder(/*dest*/);

    throw new IllegalArgumentException("required parameters are: [db, validjson]");
  }
  
static private final int parmsCount=2;
public DbCreate  db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public DbCreate  validjson(java.lang.String string){parms.put(DbKeys.etype.validjson,string);return this;}

}
java.lang.String DocFetch( java.lang.String db, java.lang.String docId );

 

public class DocFetch extends DbKeysBuilder<java.lang.String> {
  private Rfc822HeaderState rfc822HeaderState;

  private DocFetch() {
  }

  static public DocFetch $() {
    return new DocFetch();
  }

  public interface DocFetchTerminalBuilder extends TerminalBuilder<java.lang.String> {
    java.lang.String pojo();Future<java.lang.String>future();
  }

  public class DocFetchActionBuilder extends ActionBuilder<java.lang.String> {
    public DocFetchActionBuilder( /*<java.lang.String>*/ ) {
      super(/*synchronousQueues*/);
    }

    @Override
    public DocFetchTerminalBuilder fire() {
      return new DocFetchTerminalBuilder() {
         public java.lang.String pojo(){ 
try {
        return (java.lang.String) rxf.server.CouchMetaDriver.DocFetch.visit();
      } catch (Exception e) {
        e.printStackTrace();   
      }         return null;} public Future<java.lang.String>future(){
    try{
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<java.lang.String>(){


  final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();

public java.lang.String call()throws Exception{ 
        
                    DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
 ActionBuilder.currentAction.set(actionBuilder);  return(java.lang.String)rxf.server.CouchMetaDriver.DocFetch.visit(dbKeysBuilder,actionBuilder);}});
}catch(Exception e){e.printStackTrace();}return null;}
      };
    }

    @Override
    public DocFetchActionBuilder state(Rfc822HeaderState state) {
      return (DocFetchActionBuilder) super.state(state);
    }

    @Override
    public DocFetchActionBuilder key(java.nio.channels.SelectionKey key) {
      return (DocFetchActionBuilder) super.key(key);
    }
  }

  @Override
  public DocFetchActionBuilder to( /*<java.lang.String>*/ ) {
    if (parms.size() == parmsCount)
      return new DocFetchActionBuilder(/*dest*/);

    throw new IllegalArgumentException("required parameters are: [db, docId]");
  }
  
static private final int parmsCount=2;
public DocFetch  db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public DocFetch  docId(java.lang.String string){parms.put(DbKeys.etype.docId,string);return this;}

}
java.lang.String RevisionFetch( java.lang.String db, java.lang.String docId );

 

public class RevisionFetch extends DbKeysBuilder<java.lang.String> {
  private Rfc822HeaderState rfc822HeaderState;

  private RevisionFetch() {
  }

  static public RevisionFetch $() {
    return new RevisionFetch();
  }

  public interface RevisionFetchTerminalBuilder extends TerminalBuilder<java.lang.String> {
     CouchTx tx();Future<java.lang.String>future();
  }

  public class RevisionFetchActionBuilder extends ActionBuilder<java.lang.String> {
    public RevisionFetchActionBuilder( /*<java.lang.String>*/ ) {
      super(/*synchronousQueues*/);
    }

    @Override
    public RevisionFetchTerminalBuilder fire() {
      return new RevisionFetchTerminalBuilder() {
         public  CouchTx tx(){try {
        return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.RevisionFetch.visit();
      } catch (Exception e) {
        e.printStackTrace();   
      } return null;} public Future<java.lang.String>future(){
    try{
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<java.lang.String>(){


  final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();

public java.lang.String call()throws Exception{ 
        
                    DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
 ActionBuilder.currentAction.set(actionBuilder);  return(java.lang.String)rxf.server.CouchMetaDriver.RevisionFetch.visit(dbKeysBuilder,actionBuilder);}});
}catch(Exception e){e.printStackTrace();}return null;}
      };
    }

    @Override
    public RevisionFetchActionBuilder state(Rfc822HeaderState state) {
      return (RevisionFetchActionBuilder) super.state(state);
    }

    @Override
    public RevisionFetchActionBuilder key(java.nio.channels.SelectionKey key) {
      return (RevisionFetchActionBuilder) super.key(key);
    }
  }

  @Override
  public RevisionFetchActionBuilder to( /*<java.lang.String>*/ ) {
    if (parms.size() == parmsCount)
      return new RevisionFetchActionBuilder(/*dest*/);

    throw new IllegalArgumentException("required parameters are: [db, docId]");
  }
  
static private final int parmsCount=2;
public RevisionFetch  db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public RevisionFetch  docId(java.lang.String string){parms.put(DbKeys.etype.docId,string);return this;}

}
rxf.server.CouchTx DocPersist( java.lang.String db, java.lang.String validjson );

 

public class DocPersist extends DbKeysBuilder<rxf.server.CouchTx> {
  private Rfc822HeaderState rfc822HeaderState;

  private DocPersist() {
  }

  static public DocPersist $() {
    return new DocPersist();
  }

  public interface DocPersistTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
     CouchTx tx();void oneWay();Future<rxf.server.CouchTx>future();
  }

  public class DocPersistActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
    public DocPersistActionBuilder( /*<rxf.server.CouchTx>*/ ) {
      super(/*synchronousQueues*/);
    }

    @Override
    public DocPersistTerminalBuilder fire() {
      return new DocPersistTerminalBuilder() {
         public  CouchTx tx(){try {
        return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.DocPersist.visit();
      } catch (Exception e) {
        e.printStackTrace();   
      } return null;} public void oneWay(){
    final DbKeysBuilder<Object>dbKeysBuilder=(DbKeysBuilder<Object>)DbKeysBuilder.get();
 final ActionBuilder<Object>actionBuilder=(ActionBuilder<Object>)ActionBuilder.get();
dbKeysBuilder.validate();
 BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable(){
 
@Override
public void run(){
    try{

      DbKeysBuilder.currentKeys.set(dbKeysBuilder);   
      ActionBuilder.currentAction.set(actionBuilder); 
rxf.server.CouchMetaDriver.DocPersist.visit(/*dbKeysBuilder,actionBuilder*/);
}catch(Exception e){
    e.printStackTrace();}
    }
    });
}public Future<rxf.server.CouchTx>future(){
    try{
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchTx>(){


  final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();

public rxf.server.CouchTx call()throws Exception{ 
        
                    DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
 ActionBuilder.currentAction.set(actionBuilder);  return(rxf.server.CouchTx)rxf.server.CouchMetaDriver.DocPersist.visit(dbKeysBuilder,actionBuilder);}});
}catch(Exception e){e.printStackTrace();}return null;}
      };
    }

    @Override
    public DocPersistActionBuilder state(Rfc822HeaderState state) {
      return (DocPersistActionBuilder) super.state(state);
    }

    @Override
    public DocPersistActionBuilder key(java.nio.channels.SelectionKey key) {
      return (DocPersistActionBuilder) super.key(key);
    }
  }

  @Override
  public DocPersistActionBuilder to( /*<rxf.server.CouchTx>*/ ) {
    if (parms.size() == parmsCount)
      return new DocPersistActionBuilder(/*dest*/);

    throw new IllegalArgumentException("required parameters are: [db, validjson]");
  }
  
static private final int parmsCount=2;
public DocPersist  db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public DocPersist  validjson(java.lang.String string){parms.put(DbKeys.etype.validjson,string);return this;}

}
java.lang.String DesignDocFetch( java.lang.String db, java.lang.String designDocId );

 

public class DesignDocFetch extends DbKeysBuilder<java.lang.String> {
  private Rfc822HeaderState rfc822HeaderState;

  private DesignDocFetch() {
  }

  static public DesignDocFetch $() {
    return new DesignDocFetch();
  }

  public interface DesignDocFetchTerminalBuilder extends TerminalBuilder<java.lang.String> {
     CouchTx tx();
  }

  public class DesignDocFetchActionBuilder extends ActionBuilder<java.lang.String> {
    public DesignDocFetchActionBuilder( /*<java.lang.String>*/ ) {
      super(/*synchronousQueues*/);
    }

    @Override
    public DesignDocFetchTerminalBuilder fire() {
      return new DesignDocFetchTerminalBuilder() {
         public  CouchTx tx(){try {
        return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.DesignDocFetch.visit();
      } catch (Exception e) {
        e.printStackTrace();   
      } return null;} 
      };
    }

    @Override
    public DesignDocFetchActionBuilder state(Rfc822HeaderState state) {
      return (DesignDocFetchActionBuilder) super.state(state);
    }

    @Override
    public DesignDocFetchActionBuilder key(java.nio.channels.SelectionKey key) {
      return (DesignDocFetchActionBuilder) super.key(key);
    }
  }

  @Override
  public DesignDocFetchActionBuilder to( /*<java.lang.String>*/ ) {
    if (parms.size() == parmsCount)
      return new DesignDocFetchActionBuilder(/*dest*/);

    throw new IllegalArgumentException("required parameters are: [db, designDocId]");
  }
  
static private final int parmsCount=2;
public DesignDocFetch  db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public DesignDocFetch  designDocId(java.lang.String string){parms.put(DbKeys.etype.designDocId,string);return this;}

}
rxf.server.CouchResultSet ViewFetch( java.lang.String db, java.lang.String view );

 

public class ViewFetch<T> extends DbKeysBuilder<rxf.server.CouchResultSet<T>> {
  private Rfc822HeaderState rfc822HeaderState;

  private ViewFetch() {
  }

  static public <T> ViewFetch<T> $() {
    return new ViewFetch<T>();
  }

  public interface ViewFetchTerminalBuilder<T> extends TerminalBuilder<rxf.server.CouchResultSet<T>> {
    CouchResultSet<T> rows();Future<rxf.server.CouchResultSet<T>>future(); void continuousFeed();

  }

  public class ViewFetchActionBuilder extends ActionBuilder<rxf.server.CouchResultSet<T>> {
    public ViewFetchActionBuilder( /*<rxf.server.CouchResultSet>*/ ) {
      super(/*synchronousQueues*/);
    }

    @Override
    public ViewFetchTerminalBuilder<T> fire() {
      return new ViewFetchTerminalBuilder<T>() {
         public CouchResultSet<T> rows(){ 
try {
  //TODO ParameterizedType for GSON
        return (CouchResultSet<T>)  BlobAntiPatternObject.GSON.fromJson( (String)rxf.server.CouchMetaDriver.ViewFetch.visit(),CouchResultSet/*<rxf.server.CouchResultSet>*/.class);
      } catch (Exception e) {
        e.printStackTrace();    
      }         
return null ;}public Future<rxf.server.CouchResultSet<T>>future(){
    try{
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchResultSet<T>>(){


  final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();

public rxf.server.CouchResultSet<T> call()throws Exception{ 
        
                    DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
 ActionBuilder.currentAction.set(actionBuilder);  return(rxf.server.CouchResultSet)rxf.server.CouchMetaDriver.ViewFetch.visit(dbKeysBuilder,actionBuilder);}});
}catch(Exception e){e.printStackTrace();}return null;} public  void continuousFeed(){throw new AbstractMethodError();} 
      };
    }

    @Override
    public ViewFetchActionBuilder state(Rfc822HeaderState state) {
      return (ViewFetchActionBuilder) super.state(state);
    }

    @Override
    public ViewFetchActionBuilder key(java.nio.channels.SelectionKey key) {
      return (ViewFetchActionBuilder) super.key(key);
    }
  }

  @Override
  public ViewFetchActionBuilder to( /*<rxf.server.CouchResultSet>*/ ) {
    if (parms.size() == parmsCount)
      return new ViewFetchActionBuilder(/*dest*/);

    throw new IllegalArgumentException("required parameters are: [db, view]");
  }
  
static private final int parmsCount=2;
public ViewFetch<T>  db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public ViewFetch<T>  view(java.lang.String string){parms.put(DbKeys.etype.view,string);return this;}

}
rxf.server.CouchTx JsonSend( java.lang.String opaque, java.lang.String validjson );

 

public class JsonSend extends DbKeysBuilder<rxf.server.CouchTx> {
  private Rfc822HeaderState rfc822HeaderState;

  private JsonSend() {
  }

  static public JsonSend $() {
    return new JsonSend();
  }

  public interface JsonSendTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
     CouchTx tx();void oneWay();CouchResultSet/*<rxf.server.CouchTx>*/ rows();Future<rxf.server.CouchTx>future(); void continuousFeed();

  }

  public class JsonSendActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
    public JsonSendActionBuilder( /*<rxf.server.CouchTx>*/ ) {
      super(/*synchronousQueues*/);
    }

    @Override
    public JsonSendTerminalBuilder fire() {
      return new JsonSendTerminalBuilder() {
         public  CouchTx tx(){try {
        return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.JsonSend.visit();
      } catch (Exception e) {
        e.printStackTrace();   
      } return null;} public void oneWay(){
    final DbKeysBuilder<Object>dbKeysBuilder=(DbKeysBuilder<Object>)DbKeysBuilder.get();
 final ActionBuilder<Object>actionBuilder=(ActionBuilder<Object>)ActionBuilder.get();
dbKeysBuilder.validate();
 BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable(){
 
@Override
public void run(){
    try{

      DbKeysBuilder.currentKeys.set(dbKeysBuilder);   
      ActionBuilder.currentAction.set(actionBuilder); 
rxf.server.CouchMetaDriver.JsonSend.visit(/*dbKeysBuilder,actionBuilder*/);
}catch(Exception e){
    e.printStackTrace();}
    }
    });
} public CouchResultSet/*<rxf.server.CouchTx>*/ rows(){ 
try {
        return (CouchResultSet/*<rxf.server.CouchTx>*/)  BlobAntiPatternObject.GSON.fromJson( (String)rxf.server.CouchMetaDriver.JsonSend.visit(),CouchResultSet/*<rxf.server.CouchTx>*/.class);
      } catch (Exception e) {
        e.printStackTrace();    
      }         
return null ;}public Future<rxf.server.CouchTx>future(){
    try{
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchTx>(){


  final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();

public rxf.server.CouchTx call()throws Exception{ 
        
                    DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
 ActionBuilder.currentAction.set(actionBuilder);  return(rxf.server.CouchTx)rxf.server.CouchMetaDriver.JsonSend.visit(dbKeysBuilder,actionBuilder);}});
}catch(Exception e){e.printStackTrace();}return null;} public  void continuousFeed(){throw new AbstractMethodError();} 
      };
    }

    @Override
    public JsonSendActionBuilder state(Rfc822HeaderState state) {
      return (JsonSendActionBuilder) super.state(state);
    }

    @Override
    public JsonSendActionBuilder key(java.nio.channels.SelectionKey key) {
      return (JsonSendActionBuilder) super.key(key);
    }
  }

  @Override
  public JsonSendActionBuilder to( /*<rxf.server.CouchTx>*/ ) {
    if (parms.size() == parmsCount)
      return new JsonSendActionBuilder(/*dest*/);

    throw new IllegalArgumentException("required parameters are: [opaque, validjson]");
  }
  
static private final int parmsCount=2;
public JsonSend  opaque(java.lang.String string){parms.put(DbKeys.etype.opaque,string);return this;}
public JsonSend  validjson(java.lang.String string){parms.put(DbKeys.etype.validjson,string);return this;}

}
rxf.server.Rfc822HeaderState BlobSend( java.lang.String db, java.lang.String docId, java.lang.String opaque, java.lang.String mimetype, java.nio.ByteBuffer blob );

 

public class BlobSend extends DbKeysBuilder<rxf.server.Rfc822HeaderState> {
  private Rfc822HeaderState rfc822HeaderState;

  private BlobSend() {
  }

  static public BlobSend $() {
    return new BlobSend();
  }

  public interface BlobSendTerminalBuilder extends TerminalBuilder<rxf.server.Rfc822HeaderState> {
     CouchTx tx();Future<rxf.server.Rfc822HeaderState>future();void oneWay();
  }

  public class BlobSendActionBuilder extends ActionBuilder<rxf.server.Rfc822HeaderState> {
    public BlobSendActionBuilder( /*<rxf.server.Rfc822HeaderState>*/ ) {
      super(/*synchronousQueues*/);
    }

    @Override
    public BlobSendTerminalBuilder fire() {
      return new BlobSendTerminalBuilder() {
         public  CouchTx tx(){try {
        return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.BlobSend.visit();
      } catch (Exception e) {
        e.printStackTrace();   
      } return null;} public Future<rxf.server.Rfc822HeaderState>future(){
    try{
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.Rfc822HeaderState>(){


  final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();

public rxf.server.Rfc822HeaderState call()throws Exception{ 
        
                    DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
 ActionBuilder.currentAction.set(actionBuilder);  return(rxf.server.Rfc822HeaderState)rxf.server.CouchMetaDriver.BlobSend.visit(dbKeysBuilder,actionBuilder);}});
}catch(Exception e){e.printStackTrace();}return null;}public void oneWay(){
    final DbKeysBuilder<Object>dbKeysBuilder=(DbKeysBuilder<Object>)DbKeysBuilder.get();
 final ActionBuilder<Object>actionBuilder=(ActionBuilder<Object>)ActionBuilder.get();
dbKeysBuilder.validate();
 BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable(){
 
@Override
public void run(){
    try{

      DbKeysBuilder.currentKeys.set(dbKeysBuilder);   
      ActionBuilder.currentAction.set(actionBuilder); 
rxf.server.CouchMetaDriver.BlobSend.visit(/*dbKeysBuilder,actionBuilder*/);
}catch(Exception e){
    e.printStackTrace();}
    }
    });
}
      };
    }

    @Override
    public BlobSendActionBuilder state(Rfc822HeaderState state) {
      return (BlobSendActionBuilder) super.state(state);
    }

    @Override
    public BlobSendActionBuilder key(java.nio.channels.SelectionKey key) {
      return (BlobSendActionBuilder) super.key(key);
    }
  }

  @Override
  public BlobSendActionBuilder to( /*<rxf.server.Rfc822HeaderState>*/ ) {
    if (parms.size() == parmsCount)
      return new BlobSendActionBuilder(/*dest*/);

    throw new IllegalArgumentException("required parameters are: [db, docId, opaque, mimetype, blob]");
  }
  
static private final int parmsCount=5;
public BlobSend  db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public BlobSend  docId(java.lang.String string){parms.put(DbKeys.etype.docId,string);return this;}
public BlobSend  opaque(java.lang.String string){parms.put(DbKeys.etype.opaque,string);return this;}
public BlobSend  mimetype(java.lang.String string){parms.put(DbKeys.etype.mimetype,string);return this;}
public BlobSend  blob(java.nio.ByteBuffer bytebuffer){parms.put(DbKeys.etype.blob,bytebuffer);return this;}

}
}
