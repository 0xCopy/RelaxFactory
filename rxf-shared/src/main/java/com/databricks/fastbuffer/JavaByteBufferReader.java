package com.databricks.fastbuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * An implementation of the ByteBufferReader using methods directly from ByteBuffer. This is used as the fallback mode
 * in case Unsafe is not supported.
 */
public class JavaByteBufferReader implements ByteBufferReader {

  private ByteBuffer buf;
  private int mark = -1;

  public JavaByteBufferReader(ByteBuffer buf) {
    this.setBuf(buf.duplicate());
    this.getBuf().order(ByteOrder.nativeOrder());
  }

  public byte get() {
    return getBuf().get();
  }

  public byte[] get(byte[] dst, int len) {
    getBuf().get(dst, 0, len);
    return dst;
  }

  public short getShort() {
    return getBuf().getShort();
  }

  public int getInt() {
    return getBuf().getInt();
  }

  public long getLong() {
    return getBuf().getLong();
  }

  public float getFloat() {
    return getBuf().getFloat();
  }

  public double getDouble() {
    return getBuf().getDouble();
  }

  public int position() {
    return getBuf().position();
  }

  public ByteBufferReader position(int newPosition) {
    getBuf().position(newPosition);
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

  public void setBuf(ByteBuffer buf) {
    this.buf = buf;
  }
}
