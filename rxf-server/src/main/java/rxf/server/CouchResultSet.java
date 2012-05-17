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

  public class tuple<T> {
    String id, key;
    public T value;
  }

  public List<tuple<T>> rows;
}