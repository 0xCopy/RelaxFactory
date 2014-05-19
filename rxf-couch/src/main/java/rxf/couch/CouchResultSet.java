package rxf.couch;

import java.util.List;
import java.util.Map;

/**
 * User: jim
 * Date: 5/16/12
 * Time: 7:56 PM
 */
public class CouchResultSet<K, V> {

  public long totalRows;
  public long offset;

  public static class tuple<K, V> {
    public String id;
    public K key;
    public V value;
    public Map<String, ?> doc;
  }

  public List<tuple<K, V>> rows;
}