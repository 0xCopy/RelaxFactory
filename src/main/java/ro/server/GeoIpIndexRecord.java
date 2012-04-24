package ro.server;

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.Collections;

import static java.lang.Math.abs;
import static one.xio.HttpMethod.UTF8;

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

  static class Geo {
    Geo(int recordNum, ByteBuffer indexBuf) {
      this.recordNum = recordNum;
      this.indexBuf = indexBuf;
    }

    int recordNum;
    ByteBuffer indexBuf;


    public Long getBlock() {
      indexBuf.position(reclen * recordNum);
      return GeoIpService.IPMASK & indexBuf.getInt();
    }


    public String getCsv(ByteBuffer csvBuf) {

      indexBuf.position(reclen * recordNum + 4);
      int anInt = indexBuf.getInt();
      ByteBuffer buffer = (ByteBuffer) csvBuf.duplicate().clear().position(anInt);
      while (buffer.hasRemaining() && '\n' != buffer.get()) ;
      return UTF8.decode((ByteBuffer) buffer.limit(buffer.position()).position(anInt)).toString().trim();

    }


  }

  static String lookup(Inet4Address byAddress, final ByteBuffer indexBuf, ByteBuffer csvBuf) {
    AbstractList<Long> abstractList = new AbstractList<Long>() {
      @Override
      public Long get(int index) {

        int anInt = indexBuf.getInt(index * reclen);
        long l = GeoIpService.IPMASK & anInt;
        return l;

      }

      @Override
      public int size() {
        int i = indexBuf.limit() / reclen;
        return i;
      }
    };
    long l = GeoIpService.IPMASK &
        ByteBuffer.wrap(byAddress.getAddress()).getInt();


    int abs = abs(Collections.binarySearch(abstractList, l));
    String csv = new Geo(abs, indexBuf).getCsv(csvBuf);
    return csv;
  }


}
