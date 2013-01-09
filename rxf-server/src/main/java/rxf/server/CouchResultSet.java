package rxf.server;

import java.util.List;

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
    String id, key;

    @Override
    public String toString() {
      return "tuple{" + "id='" + id + '\'' + ", key='" + key + '\'' + ", value=" + value + '}';
    }

    public T value;
  }

  public List<tuple<T>> rows;
}