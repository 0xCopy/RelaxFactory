package rxf.server;

import java.util.List;
import java.util.Map;

/**
 * User: jim
 * Date: 5/16/12
 * Time: 7:56 PM
 */
public class CouchResultSet<T> {

  public long totalRows;
  public long offset;

  public static class tuple<T> {
    public String id, key;
    public T value;
    public Map<String, ?> doc;
  }

  public List<tuple<T>> rows;
}