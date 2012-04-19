package ro.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ro.model.RoSession;

import static one.xio.HttpMethod.UTF8;
import static ro.server.CouchChangesClient.GSON;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class KernelImpl {

  public static final RoSessionLocator RO_SESSION_LOCATOR = new RoSessionLocator();
  public static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
  public static final Charset UTF_8 = UTF8;


  static public RoSession getCurrentSession() {
    Object o = new Object() {
    };
     {
      return RO_SESSION_LOCATOR.create(RoSession.class);
    }
  }

  //test
  public static void main(String... args) throws InterruptedException, IOException, ExecutionException {
    EXECUTOR_SERVICE.submit(new Runnable() {
      @Override
      public void run() {
        String id;
        {
          RoSessionLocator roSessionLocator = new RoSessionLocator();
          RoSession roSession = roSessionLocator.create(RoSession.class);
          id = roSession.getId();
          String s = GSON.toJson(roSession);
          System.err.println("created: " + s);
        }

        {
          RoSessionLocator roSessionLocator = new RoSessionLocator();
          RoSession roSession = roSessionLocator.find(RoSession.class, id);
          String s = GSON.toJson(roSession);
          System.err.println("find: " + s);

        }
      }
    });

    RfPostWrapper.startServer(args);

  }
}

