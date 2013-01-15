package rxf.server;

import java.nio.ByteBuffer;

/**
 * User: jim
 * Date: 4/24/12
 * Time: 10:30 AM
 */
enum GeoIpIndexRecord {
  ipBlock(4), offset(4);

  int len;
  int pos;
  static int reclen;

  GeoIpIndexRecord(int len) {
    this.len = len;
    init(len);
  }

  private void init(int len) {
    pos = reclen;
    reclen += len;
  }

  static int count(ByteBuffer b) {
    return b.limit() / reclen;
  }

}
