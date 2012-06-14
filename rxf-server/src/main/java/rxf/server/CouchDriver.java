
package rxf.server;
//generated
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * generated drivers
 */
public interface CouchDriver{CouchTx DbCreate( String db, String validjson );



  public class DbCreate extends DbKeysBuilder<CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;
  
    private DbCreate() {
    }
  
    static public DbCreate $() {
      return new DbCreate();
    }
  
    public interface DbCreateTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();void oneWay();
    }
  
    public class DbCreateActionBuilder extends ActionBuilder<CouchTx> {
      public DbCreateActionBuilder( /*<CouchTx>*/ ) {
        super(/*synchronousQueues*/);
      }
  
      @Override
      public DbCreateTerminalBuilder fire() {
        return new DbCreateTerminalBuilder() {
          public  CouchTx tx(){try {
            return (CouchTx) CouchMetaDriver.DbCreate.visit();
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
                  CouchMetaDriver.DbCreate.visit(/*dbKeysBuilder,actionBuilder*/);
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
      public DbCreateActionBuilder key(SelectionKey key) {
        return (DbCreateActionBuilder) super.key(key);
      }
    }
  
    @Override
    public DbCreateActionBuilder to( /*<CouchTx>*/ ) {
      if (parms.size() == parmsCount)
        return new DbCreateActionBuilder(/*dest*/);
  
      throw new IllegalArgumentException("required parameters are: [db]");
    }
  
    static private final int parmsCount=1;
    public DbCreate  db(String string){parms.put(DbKeys.etype.db,string);return this;}
  
  }
  String DocFetch( String db, String docId );
  


  public class DocFetch extends DbKeysBuilder<String> {
    private Rfc822HeaderState rfc822HeaderState;
  
    private DocFetch() {
    }
  
    static public DocFetch $() {
      return new DocFetch();
    }
  
    public interface DocFetchTerminalBuilder extends TerminalBuilder<String> {
      String pojo();Future<String>future();
    }
  
    public class DocFetchActionBuilder extends ActionBuilder<String> {
      public DocFetchActionBuilder( /*<String>*/ ) {
        super(/*synchronousQueues*/);
      }
  
      @Override
      public DocFetchTerminalBuilder fire() {
        return new DocFetchTerminalBuilder() {
          public String pojo(){ 
            try {
              return (String) CouchMetaDriver.DocFetch.visit();
            } catch (Exception e) {
              e.printStackTrace();   
            }         return null;} public Future<String>future(){
              try{
                BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<String>(){
  
  
                  final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
                  final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();
  
                  public String call()throws Exception{ 
  
                    DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
                    ActionBuilder.currentAction.set(actionBuilder);  return(String)CouchMetaDriver.DocFetch.visit(dbKeysBuilder,actionBuilder);}});
              }catch(Exception e){e.printStackTrace();}return null;}
        };
      }
  
      @Override
      public DocFetchActionBuilder state(Rfc822HeaderState state) {
        return (DocFetchActionBuilder) super.state(state);
      }
  
      @Override
      public DocFetchActionBuilder key(SelectionKey key) {
        return (DocFetchActionBuilder) super.key(key);
      }
    }
  
    @Override
    public DocFetchActionBuilder to( /*<String>*/ ) {
      if (parms.size() == parmsCount)
        return new DocFetchActionBuilder(/*dest*/);
  
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }
  
    static private final int parmsCount=2;
    public DocFetch  db(String string){parms.put(DbKeys.etype.db,string);return this;}
    public DocFetch  docId(String string){parms.put(DbKeys.etype.docId,string);return this;}
  
  }
  String RevisionFetch( String db, String docId );



  public class RevisionFetch extends DbKeysBuilder<String> {
    private Rfc822HeaderState rfc822HeaderState;
  
    private RevisionFetch() {
    }
  
    static public RevisionFetch $() {
      return new RevisionFetch();
    }
  
    public interface RevisionFetchTerminalBuilder extends TerminalBuilder<String> {
      CouchTx tx();Future<String>future();
    }
  
    public class RevisionFetchActionBuilder extends ActionBuilder<String> {
      public RevisionFetchActionBuilder( /*<String>*/ ) {
        super(/*synchronousQueues*/);
      }
  
      @Override
      public RevisionFetchTerminalBuilder fire() {
        return new RevisionFetchTerminalBuilder() {
          public  CouchTx tx(){try {
            return (CouchTx) CouchMetaDriver.RevisionFetch.visit();
          } catch (Exception e) {
            e.printStackTrace();   
          } return null;} public Future<String>future(){
            try{
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<String>(){
  
  
                final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
                final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();
  
                public String call()throws Exception{ 
  
                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
                  ActionBuilder.currentAction.set(actionBuilder);  return(String)CouchMetaDriver.RevisionFetch.visit(dbKeysBuilder,actionBuilder);}});
            }catch(Exception e){e.printStackTrace();}return null;}
        };
      }
  
      @Override
      public RevisionFetchActionBuilder state(Rfc822HeaderState state) {
        return (RevisionFetchActionBuilder) super.state(state);
      }
  
      @Override
      public RevisionFetchActionBuilder key(SelectionKey key) {
        return (RevisionFetchActionBuilder) super.key(key);
      }
    }
  
    @Override
    public RevisionFetchActionBuilder to( /*<String>*/ ) {
      if (parms.size() == parmsCount)
        return new RevisionFetchActionBuilder(/*dest*/);
  
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }
  
    static private final int parmsCount=2;
    public RevisionFetch  db(String string){parms.put(DbKeys.etype.db,string);return this;}
    public RevisionFetch  docId(String string){parms.put(DbKeys.etype.docId,string);return this;}
  
  }
  CouchTx DocPersist( String db, String validjson );



  public class DocPersist extends DbKeysBuilder<CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;
  
    private DocPersist() {
    }
  
    static public DocPersist $() {
      return new DocPersist();
    }
  
    public interface DocPersistTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();void oneWay();Future<CouchTx>future();
    }
  
    public class DocPersistActionBuilder extends ActionBuilder<CouchTx> {
      public DocPersistActionBuilder( /*<CouchTx>*/ ) {
        super(/*synchronousQueues*/);
      }
  
      @Override
      public DocPersistTerminalBuilder fire() {
        return new DocPersistTerminalBuilder() {
          public  CouchTx tx(){try {
            return (CouchTx) CouchMetaDriver.DocPersist.visit();
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
                  CouchMetaDriver.DocPersist.visit(/*dbKeysBuilder,actionBuilder*/);
                }catch(Exception e){
                  e.printStackTrace();}
              }
            });
          }public Future<CouchTx>future(){
            try{
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<CouchTx>(){
  
  
                final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
                final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();
  
                public CouchTx call()throws Exception{ 
  
                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
                  ActionBuilder.currentAction.set(actionBuilder);  return(CouchTx)CouchMetaDriver.DocPersist.visit(dbKeysBuilder,actionBuilder);}});
            }catch(Exception e){e.printStackTrace();}return null;}
        };
      }
  
      @Override
      public DocPersistActionBuilder state(Rfc822HeaderState state) {
        return (DocPersistActionBuilder) super.state(state);
      }
  
      @Override
      public DocPersistActionBuilder key(SelectionKey key) {
        return (DocPersistActionBuilder) super.key(key);
      }
    }
  
    @Override
    public DocPersistActionBuilder to( /*<CouchTx>*/ ) {
      if (parms.size() == parmsCount)
        return new DocPersistActionBuilder(/*dest*/);
  
      throw new IllegalArgumentException("required parameters are: [db, validjson]");
    }
  
    static private final int parmsCount=2;
    public DocPersist  db(String string){parms.put(DbKeys.etype.db,string);return this;}
    public DocPersist  validjson(String string){parms.put(DbKeys.etype.validjson,string);return this;}
  
  }
  String DesignDocFetch( String db, String designDocId );



  public class DesignDocFetch extends DbKeysBuilder<String> {
    private Rfc822HeaderState rfc822HeaderState;
  
    private DesignDocFetch() {
    }
  
    static public DesignDocFetch $() {
      return new DesignDocFetch();
    }
  
    public interface DesignDocFetchTerminalBuilder extends TerminalBuilder<String> {
      CouchTx tx();
    }
  
    public class DesignDocFetchActionBuilder extends ActionBuilder<String> {
      public DesignDocFetchActionBuilder( /*<String>*/ ) {
        super(/*synchronousQueues*/);
      }
  
      @Override
      public DesignDocFetchTerminalBuilder fire() {
        return new DesignDocFetchTerminalBuilder() {
          public  CouchTx tx(){try {
            return (CouchTx) CouchMetaDriver.DesignDocFetch.visit();
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
      public DesignDocFetchActionBuilder key(SelectionKey key) {
        return (DesignDocFetchActionBuilder) super.key(key);
      }
    }
  
    @Override
    public DesignDocFetchActionBuilder to( /*<String>*/ ) {
      if (parms.size() == parmsCount)
        return new DesignDocFetchActionBuilder(/*dest*/);
  
      throw new IllegalArgumentException("required parameters are: [db, designDocId]");
    }
  
    static private final int parmsCount=2;
    public DesignDocFetch  db(String string){parms.put(DbKeys.etype.db,string);return this;}
    public DesignDocFetch  designDocId(String string){parms.put(DbKeys.etype.designDocId,string);return this;}
  
  }
  CouchResultSet ViewFetch( String db, String view );



  public class ViewFetch<T> extends DbKeysBuilder<CouchResultSet<T>> {
    private Rfc822HeaderState rfc822HeaderState;
    private Class<T> type;
  
    private ViewFetch() {
    }
  
    static public <T> ViewFetch<T> $() {
      return new ViewFetch<T>();
    }
  
    public interface ViewFetchTerminalBuilder<T> extends TerminalBuilder<CouchResultSet<T>> {
      CouchResultSet<T> rows();Future<CouchResultSet<T>>future(); void continuousFeed();
  
    }
  
    public class ViewFetchActionBuilder extends ActionBuilder<CouchResultSet<T>> {
      public ViewFetchActionBuilder( /*<CouchResultSet>*/ ) {
        super(/*synchronousQueues*/);
      }
  
      @Override
      public ViewFetchTerminalBuilder<T> fire() {
        return new ViewFetchTerminalBuilder<T>() {
          public CouchResultSet<T> rows(){ 
            try {
              //TODO ParameterizedType for GSON
              return (CouchResultSet<T>)  BlobAntiPatternObject.GSON.fromJson( (String)CouchMetaDriver.ViewFetch.visit(),createResultSetType());
            } catch (Exception e) {
              e.printStackTrace();    
            }         
            return null ;}public Future<CouchResultSet<T>>future(){
              try{
                BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<CouchResultSet<T>>(){
  
  
                  final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
                  final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();
  
                  public CouchResultSet<T> call()throws Exception{ 
  
                    DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
                    ActionBuilder.currentAction.set(actionBuilder);  return(CouchResultSet)CouchMetaDriver.ViewFetch.visit(dbKeysBuilder,actionBuilder);}});
              }catch(Exception e){e.printStackTrace();}return null;} public  void continuousFeed(){throw new AbstractMethodError();} 
              private Type createResultSetType() {
                return new ParameterizedType() {
                  @Override
                  public Type getRawType() {
                    return CouchResultSet.class;
                  }
                  @Override
                  public Type getOwnerType() {
                    return null;
                  }
                  @Override
                  public Type[] getActualTypeArguments() {
                    return new Type[] {type};
                  }
                };
              }
        };
      }
  
      @Override
      public ViewFetchActionBuilder state(Rfc822HeaderState state) {
        return (ViewFetchActionBuilder) super.state(state);
      }
  
      @Override
      public ViewFetchActionBuilder key(SelectionKey key) {
        return (ViewFetchActionBuilder) super.key(key);
      }
    }
  
    @Override
    public ViewFetchActionBuilder to( /*<CouchResultSet>*/ ) {
      if (parms.size() == parmsCount)
        return new ViewFetchActionBuilder(/*dest*/);
  
      throw new IllegalArgumentException("required parameters are: [db, view]");
    }
  
    static private final int parmsCount=2;
    public ViewFetch<T>  db(String string, Class<T> clazz){parms.put(DbKeys.etype.db,string);this.type = clazz; return this;}
    public ViewFetch<T>  view(String string){parms.put(DbKeys.etype.view,string);return this;}
  
  }
  CouchTx JsonSend( String opaque, String validjson );



  public class JsonSend extends DbKeysBuilder<CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;
  
    private JsonSend() {
    }
  
    static public JsonSend $() {
      return new JsonSend();
    }
  
    public interface JsonSendTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();void oneWay();CouchResultSet/*<CouchTx>*/ rows();Future<CouchTx>future(); void continuousFeed();
  
    }
  
    public class JsonSendActionBuilder extends ActionBuilder<CouchTx> {
      public JsonSendActionBuilder( /*<CouchTx>*/ ) {
        super(/*synchronousQueues*/);
      }
  
      @Override
      public JsonSendTerminalBuilder fire() {
        return new JsonSendTerminalBuilder() {
          public  CouchTx tx(){try {
            return (CouchTx) CouchMetaDriver.JsonSend.visit();
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
                  CouchMetaDriver.JsonSend.visit(/*dbKeysBuilder,actionBuilder*/);
                }catch(Exception e){
                  e.printStackTrace();}
              }
            });
          } public CouchResultSet/*<CouchTx>*/ rows(){ 
            try {
              return (CouchResultSet/*<CouchTx>*/)  BlobAntiPatternObject.GSON.fromJson( (String)CouchMetaDriver.JsonSend.visit(),CouchResultSet/*<CouchTx>*/.class);
            } catch (Exception e) {
              e.printStackTrace();    
            }         
            return null ;}public Future<CouchTx>future(){
              try{
                BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<CouchTx>(){
  
  
                  final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
                  final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();
  
                  public CouchTx call()throws Exception{ 
  
                    DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
                    ActionBuilder.currentAction.set(actionBuilder);  return(CouchTx)CouchMetaDriver.JsonSend.visit(dbKeysBuilder,actionBuilder);}});
              }catch(Exception e){e.printStackTrace();}return null;} public  void continuousFeed(){throw new AbstractMethodError();} 
        };
      }
  
      @Override
      public JsonSendActionBuilder state(Rfc822HeaderState state) {
        return (JsonSendActionBuilder) super.state(state);
      }
  
      @Override
      public JsonSendActionBuilder key(SelectionKey key) {
        return (JsonSendActionBuilder) super.key(key);
      }
    }
  
    @Override
    public JsonSendActionBuilder to( /*<CouchTx>*/ ) {
      if (parms.size() == parmsCount)
        return new JsonSendActionBuilder(/*dest*/);
  
      throw new IllegalArgumentException("required parameters are: [opaque, validjson]");
    }
  
    static private final int parmsCount=2;
    public JsonSend  opaque(String string){parms.put(DbKeys.etype.opaque,string);return this;}
    public JsonSend  validjson(String string){parms.put(DbKeys.etype.validjson,string);return this;}
  
  }
  Rfc822HeaderState BlobSend( String db, String docId, String opaque, String mimetype, ByteBuffer blob );



  public class BlobSend extends DbKeysBuilder<Rfc822HeaderState> {
  private Rfc822HeaderState rfc822HeaderState;

  private BlobSend() {
  }

  static public BlobSend $() {
    return new BlobSend();
  }

  public interface BlobSendTerminalBuilder extends TerminalBuilder<Rfc822HeaderState> {
    CouchTx tx();Future<Rfc822HeaderState>future();void oneWay();
  }

  public class BlobSendActionBuilder extends ActionBuilder<Rfc822HeaderState> {
    public BlobSendActionBuilder( /*<Rfc822HeaderState>*/ ) {
      super(/*synchronousQueues*/);
    }

    @Override
    public BlobSendTerminalBuilder fire() {
      return new BlobSendTerminalBuilder() {
        public  CouchTx tx(){try {
          return (CouchTx) CouchMetaDriver.BlobSend.visit();
        } catch (Exception e) {
          e.printStackTrace();   
        } return null;} public Future<Rfc822HeaderState>future(){
          try{
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<Rfc822HeaderState>(){


              final  DbKeysBuilder dbKeysBuilder=(DbKeysBuilder )DbKeysBuilder.get();
              final ActionBuilder actionBuilder=(ActionBuilder )ActionBuilder.get();

              public Rfc822HeaderState call()throws Exception{ 

                DbKeysBuilder.currentKeys.set(dbKeysBuilder);  
                ActionBuilder.currentAction.set(actionBuilder);  return(Rfc822HeaderState)CouchMetaDriver.BlobSend.visit(dbKeysBuilder,actionBuilder);}});
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
                  CouchMetaDriver.BlobSend.visit(/*dbKeysBuilder,actionBuilder*/);
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
    public BlobSendActionBuilder key(SelectionKey key) {
      return (BlobSendActionBuilder) super.key(key);
    }
  }

  @Override
  public BlobSendActionBuilder to( /*<Rfc822HeaderState>*/ ) {
    if (parms.size() == parmsCount)
      return new BlobSendActionBuilder(/*dest*/);

    throw new IllegalArgumentException("required parameters are: [db, docId, opaque, mimetype, blob]");
  }

  static private final int parmsCount=5;
  public BlobSend  db(String string){parms.put(DbKeys.etype.db,string);return this;}
  public BlobSend  docId(String string){parms.put(DbKeys.etype.docId,string);return this;}
  public BlobSend  opaque(String string){parms.put(DbKeys.etype.opaque,string);return this;}
  public BlobSend  mimetype(String string){parms.put(DbKeys.etype.mimetype,string);return this;}
  public BlobSend  blob(ByteBuffer bytebuffer){parms.put(DbKeys.etype.blob,bytebuffer);return this;}

}
}
