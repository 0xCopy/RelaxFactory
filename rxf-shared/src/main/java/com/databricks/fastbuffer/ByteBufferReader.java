package com.databricks.fastbuffer;


import java.nio.ByteBuffer;

public interface ByteBufferReader {

    byte get();

    byte[] get(byte[] dst, int len);

    short getShort();

    int getInt();

    long getLong();

    float getFloat();

    double getDouble();

    int position();

    ByteBufferReader position(int newPosition);


    default int mark() {
        int position = position();
        setMark(position);
        return position;
    }

    default ByteBufferReader reset() {
        return position(getMark());
    }

    default ByteBuffer commit() {
        return (ByteBuffer) getBuf().position(position());
    }

    ByteBuffer getBuf();

    int getMark();

    void setMark(int mark);


}
