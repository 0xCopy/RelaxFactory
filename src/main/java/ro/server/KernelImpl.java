package ro.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import one.xio.HttpHeaders;
import one.xio.HttpMethod;
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
  public static final String MYSESSIONSTRING = "mysessionstring";


  static public RoSession getCurrentSession() {
    ByteBuffer hb = RfPostWrapper.ThreadLocalHeaders.get();
    Map o = (Map) RfPostWrapper.ThreadLocalSetCookies.get();

    RoSession roSession = null;
    Map<String, int[]> headers = HttpHeaders.getHeaders(hb);
    String id = null;
    try {
      if (headers.containsKey("Cookie")) {
        int[] cookieses = headers.get("Cookie");
        String coo = HttpMethod.UTF8.decode((ByteBuffer) hb.limit(cookieses[1]).position(cookieses[0])).toString().trim();

        String[] split = coo.split(";");
        for (String s : split) {
          String[] split1 = s.split("=");
          if (MYSESSIONSTRING.equals(split1[0])) {
            id = split1[1].trim();
            roSession = RO_SESSION_LOCATOR.find(RoSession.class, id);
            break;
          }
        }
      }
      return roSession;
    } catch (Throwable e) {
      System.err.println("cookie failure on "+id);
      e.printStackTrace();  //todo: verify for a purpose
    }
    roSession = RO_SESSION_LOCATOR.create(RoSession.class);
    if (null == o) {
     Map<String, String> value = new TreeMap<String, String>();
      value.put(MYSESSIONSTRING, roSession.getId());
      RfPostWrapper.ThreadLocalSetCookies.set(value);
    }

    return roSession;
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

