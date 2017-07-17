package com.databricks.fastbuffer;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * An implementation of the ByteBufferReader using sun.misc.Unsafe. This provides very high throughput read of various
 * primitive types from a ByteBuffer, but can potentially crash the JVM if the implementation is faulty.
 */
public class UnsafeDirectByteBufferReader implements ByteBufferReader {

  private long baseOffset;
  private long offset;
  private ByteBuffer buf;
  private int mark = -1;

  public UnsafeDirectByteBufferReader(ByteBuffer buf) {
    this.buf = buf;
    baseOffset = getMemoryAddress(this.buf);
    offset = baseOffset;
  }

  public byte get() {
    byte v = Unsafe.UNSAFE.getByte(offset);
    offset += 1;
    return v;
  }

  public byte[] get(byte[] dst, int len) {
    Unsafe.UNSAFE.copyMemory(null, offset, dst, Unsafe.BYTE_ARRAY_BASE_OFFSET, len);
    return dst;
  }

  public short getShort() {
    short v = Unsafe.UNSAFE.getShort(offset);
    offset += 2;
    return v;
  }

  public int getInt() {
    int v = Unsafe.UNSAFE.getInt(offset);
    offset += 4;
    return v;
  }

  public long getLong() {
    long v = Unsafe.UNSAFE.getLong(offset);
    offset += 8;
    return v;
  }

  public float getFloat() {
    float v = Unsafe.UNSAFE.getFloat(offset);
    offset += 4;
    return v;
  }

  public double getDouble() {
    double v = Unsafe.UNSAFE.getDouble(offset);
    offset += 8;
    return v;
  }

  public int position() {
    return (int) (offset - baseOffset);
  }

  public ByteBufferReader position(int newPosition) {
    offset = baseOffset + newPosition;
    return this;
  }

  @Override
  public ByteBuffer getBuf() {
    return buf;
  }

  @Override
  public int getMark() {
    return mark;
  }

  @Override
  public void setMark(int mark) {
    this.mark = mark;
  }

  static Field addressFieldMethod;
  static {
    try {
      addressFieldMethod = java.nio.Buffer.class.getDeclaredField("address");
      addressFieldMethod.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new UnsupportedOperationException(e);

    }
  }

  private static long getMemoryAddress(ByteBuffer buf) throws UnsupportedOperationException {
    long address;
    try {
      address = addressFieldMethod.getLong(buf);
    } catch (Exception e) {
      throw new UnsupportedOperationException(e);
    }
    return address;
  }
}
