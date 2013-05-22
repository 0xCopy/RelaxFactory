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

  @Override
  public String toString() {
    return "CouchResultSet{" + "totalRows=" + totalRows + ", offset=" + offset + ", rows=" + rows
        + '}';
  }

  public static class tuple<T> {
    String id;

    //		Object key;

    @Override
    public String toString() {
      return "tuple{" + "id='" + id + '\'' + ", key='" + null + '\'' + ", value=" + value + '}';
    }

    public T value;
    public Map<String, ?> doc;
  }

  public List<tuple<T>> rows;
}