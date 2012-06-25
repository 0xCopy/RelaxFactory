package rxf.server;

public class CouchServiceFactoryTest {
/*
  private static ScheduledExecutorService exec;
  static boolean first = true;

  @BeforeClass
  public static void setUp() throws Exception {
    try {
      if (first) {
        first = false;
        tearDown();
      }
    } catch (Exception e) {
      e.printStackTrace();  //todo: verify for a purpose
    } finally {
    }

    BlobAntiPatternObject.DEBUG_SENDJSON = true;
    HttpMethod.killswitch = false;
    exec = Executors.newSingleThreadScheduledExecutor();
    exec.submit(new Runnable() {
      public void run() {
        AsioVisitor topLevel = new ProtocolMethodDispatch();
        try {
          HttpMethod.init(new String[]{}, topLevel, 1000);
        } catch (Exception e) {
        }
      }
    });
    CouchDriver.DbCreate.$().db(DB).to().fire().tx();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    CouchDriver.DbDelete.$().db(DB).to().fire().tx();
    try {
      HttpMethod.killswitch = true;
      HttpMethod.getSelector().close();
//      HttpMethod.broke = null;
      exec.shutdown();
      //Thread.sleep(4000);//more than 3 seconds, standard timeout
    } catch (Exception ignore) {
    }
  }

*/

}