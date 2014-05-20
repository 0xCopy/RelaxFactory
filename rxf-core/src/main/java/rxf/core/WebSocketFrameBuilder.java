package rxf.core;

import java.util.Random;

public class WebSocketFrameBuilder {
  private byte[] maskingKey = null;
  private long payloadLength = 0;
  private boolean isMasked = false;
  private WebSocketFrame.OpCode opcode = WebSocketFrame.OpCode.text;
  private boolean isFin = true;

  public WebSocketFrameBuilder setMaskingKey(byte[] maskingKey) {

    if (maskingKey == null) {
      byte[] bytes = new byte[4];
      new Random().nextBytes(bytes);
      maskingKey = bytes;
    }
    this.maskingKey = maskingKey;

    return this;
  }

  public WebSocketFrameBuilder setPayloadLength(long payloadLength) {
    this.payloadLength = payloadLength;
    return this;
  }

  /**
   * @param isMasked
   *            if true, also sets a random mask when no mask is set.
   * @return this
   */
  public WebSocketFrameBuilder setIsMasked(boolean isMasked) {
    this.isMasked = isMasked;
    if (isMasked && maskingKey == null)
      setMaskingKey(null);
    return this;
  }

  public WebSocketFrameBuilder setOpcode(WebSocketFrame.OpCode opcode) {
    this.opcode = opcode;
    return this;
  }

  public WebSocketFrameBuilder setIsFin(boolean isFin) {
    this.isFin = isFin;
    return this;
  }

  public WebSocketFrame createWebSocketFrame() {
    return new WebSocketFrame(maskingKey, payloadLength, isMasked, opcode, isFin);
  }
}