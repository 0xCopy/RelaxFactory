package com.databricks.fastbuffer;

import java.nio.ByteBuffer;

/**
 * An implementation of the ByteBufferReader using sun.misc.Unsafe. This provides very high throughput read of various
 * primitive types from a HeapByteBuffer, but can potentially crash the JVM if the implementation is faulty.
 */
public class UnsafeHeapByteBufferReader implements ByteBufferReader {

  private long offset;
  private byte[] array;
  private ByteBuffer buf;
  private int mark;

  public UnsafeHeapByteBufferReader(ByteBuffer buf) {
    this.buf = buf;

    if (!buf.hasArray()) {
      throw new UnsupportedOperationException("buf (" + buf + ") must have a backing array");
    }
    offset = Unsafe.BYTE_ARRAY_BASE_OFFSET;
    array = buf.array();
  }

  public byte get() {
    byte v = Unsafe.UNSAFE.getByte(array, offset);
    offset += 1;
    return v;
  }

  public byte[] get(byte[] dst, int len) {
    Unsafe.UNSAFE.copyMemory(array, offset, dst, Unsafe.BYTE_ARRAY_BASE_OFFSET, len);
    return dst;
  }

  public short getShort() {
    short v = Unsafe.UNSAFE.getShort(array, offset);
    offset += 2;
    return v;
  }

  public int getInt() {
    int v = Unsafe.UNSAFE.getInt(array, offset);
    offset += 4;
    return v;
  }

  public long getLong() {
    long v = Unsafe.UNSAFE.getLong(array, offset);
    offset += 8;
    return v;
  }

  public float getFloat() {
    float v = Unsafe.UNSAFE.getFloat(array, offset);
    offset += 4;
    return v;
  }

  public double getDouble() {
    double v = Unsafe.UNSAFE.getDouble(array, offset);
    offset += 8;
    return v;
  }

  public int position() {
    return (int) (offset - Unsafe.BYTE_ARRAY_BASE_OFFSET);
  }

  public ByteBufferReader position(int newPosition) {
    offset = Unsafe.BYTE_ARRAY_BASE_OFFSET + newPosition;
    return this;
  }

  public ByteBuffer getBuf() {
    return buf;
  }

  public int getMark() {
    return mark;
  }

  public void setMark(int mark) {
    this.mark = mark;
  }
}
