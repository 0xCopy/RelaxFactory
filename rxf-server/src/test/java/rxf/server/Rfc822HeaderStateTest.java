package rxf.server;

import java.nio.ByteBuffer;

import static one.xio.HttpMethod.UTF8;
import static org.junit.Assert.*;

import org.junit.Test;

public class Rfc822HeaderStateTest {

  @Test
  public void testAppendHeadersOne() {
    Rfc822HeaderState state = new Rfc822HeaderState("One");
    assertEquals(1, state.headerInterest.length);
    state.headers("Two");
    assertEquals(2, state.headerInterest.length);
    assertArrayEquals(new String[]{"One", "Two"}, state.headers());
  }
  @Test
  public void testAppendHeadersMany() {
    Rfc822HeaderState state = new Rfc822HeaderState("One");
    assertEquals(1, state.headerInterest.length);
    state.headerInterest("Two", "Three");
    assertEquals(3, state.headerInterest.length);
    assertArrayEquals(new String[]{"One", "Two", "Three"}, state.headers());
  }
  
  @Test
  public void testAsRequestHeaderByteBuffer() {
    Rfc822HeaderState req = new Rfc822HeaderState();
    req.methodProtocol("VERB").pathResCode("/noun").protocolStatus("HTTP/1.0").headerString("Header", "value").headerString("Header2", "value2");
    ByteBuffer buf = req.asRequestHeaderByteBuffer();
    String result = UTF8.decode(buf.duplicate()).toString();
    
    assertEquals("VERB /noun HTTP/1.0\r\nHeader: value\r\nHeader2: value2\r\n\r\n", result);
  }
  @Test
  public void testApplySimpleResponse() {
    ByteBuffer simpleResponse = ByteBuffer.wrap("HTTP/1.0 200 OK\r\nServer: NotReallyAServer\r\n\r\n".getBytes());
    
    Rfc822HeaderState state = new Rfc822HeaderState();
    state.headers("Server");
    state.apply(simpleResponse);
    
    assertEquals("HTTP/1.0", state.methodProtocol());
    assertEquals("200", state.pathResCode());
    assertEquals("OK", state.protocolStatus());
    assertEquals("NotReallyAServer", state.headerString("Server"));
  }
  
  @Test
  public void testApplySimpleRequest() {
    ByteBuffer simpleRequest = ByteBuffer.wrap("GET /file/from/path.suffix HTTP/1.0\r\nContent-Type: application/json\r\n\r\n".getBytes());
    
    Rfc822HeaderState state = new Rfc822HeaderState("Content-Type");
    state.apply(simpleRequest);
    
    assertEquals("GET", state.methodProtocol());
    assertEquals("/file/from/path.suffix", state.pathResCode());
    assertEquals("HTTP/1.0", state.protocolStatus());
    assertEquals("application/json", state.headerString("Content-Type"));
  }
  @Test
  public void testAsResponseHeaderByteBuffer() {
    Rfc822HeaderState resp = new Rfc822HeaderState();
    resp.methodProtocol("HTTP/1.0").pathResCode("501").protocolStatus("Unsupported Method").headerString("Connection", "close");
    ByteBuffer buf = resp.asResponseHeaderByteBuffer();
    String result = UTF8.decode(buf).toString();
    assertEquals("HTTP/1.0 501 Unsupported Method\r\nConnection: close\r\n\r\n", result);
  }
}
