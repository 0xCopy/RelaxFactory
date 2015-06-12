/*
 * ==================================================================== Licensed to the Apache Software Foundation (ASF)
 * under one or more contributor license agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on behalf of the Apache Software
 * Foundation. For more information on the Apache Software Foundation, please see <http://www.apache.org/>.
 */
package org.apache.http.benchmark.netty;

import static org.jboss.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;

class RandomDataHandler extends SimpleChannelUpstreamHandler {

  private HttpRequest request;
  private boolean readingChunks;
  private int count;

  public RandomDataHandler() {
    super();
  }

  @Override
  public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
      throws Exception {
    count = 100;
    if (!readingChunks) {
      HttpRequest request = this.request = (HttpRequest) e.getMessage();
      String target = request.getUri();

      int idx = target.indexOf('?');
      if (idx != -1) {
        String s = target.substring(idx + 1);
        if (s.startsWith("c=")) {
          s = s.substring(2);
          try {
            count = Integer.parseInt(s);
          } catch (NumberFormatException ex) {
            writeError(e, HttpResponseStatus.BAD_REQUEST, ex.getMessage());
            return;
          }
        }
      }

      if (is100ContinueExpected(request)) {
        send100Continue(e);
      }

      if (request.isChunked()) {
        readingChunks = true;
      } else {
        writeResponse(e);
      }
    } else {
      HttpChunk chunk = (HttpChunk) e.getMessage();
      if (chunk.isLast()) {
        readingChunks = false;
        writeResponse(e);
      }
    }
  }

  private void writeError(final MessageEvent e, final HttpResponseStatus status,
      final String message) {
    // Decide whether to close the connection or not.
    boolean keepAlive = isKeepAlive(request);

    // Build the response object.
    HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
    response.setContent(ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8));
    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

    if (keepAlive) {
      // Add 'Content-Length' header only for a keep-alive connection.
      response.headers().set(CONTENT_LENGTH, response.getContent().readableBytes());
      // Add keep alive header as per:
      // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    // Write the response.
    ChannelFuture future = e.getChannel().write(response);

    // Close the non-keep-alive connection after the write operation is done.
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }

  private void writeResponse(final MessageEvent e) {
    // Decide whether to close the connection or not.
    boolean keepAlive = isKeepAlive(request);

    // Build the response object.
    HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);

    byte[] buf = new byte[count];
    int r = Math.abs(buf.hashCode());
    for (int i = 0; i < count; i++) {
      buf[i] = (byte) ((r + i) % 96 + 32);
    }
    response.setContent(ChannelBuffers.copiedBuffer(buf));

    response.headers().set(CONTENT_TYPE, "text/plain");
    if (keepAlive) {
      // Add 'Content-Length' header only for a keep-alive connection.
      response.headers().set(CONTENT_LENGTH, response.getContent().readableBytes());
      // Add keep alive header as per:
      // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    }

    // Write the response.
    ChannelFuture future = e.getChannel().write(response);

    // Close the non-keep-alive connection after the write operation is done.
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }

  private static void send100Continue(final MessageEvent e) {
    HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
    e.getChannel().write(response);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e)
      throws Exception {
    e.getCause().printStackTrace();
    e.getChannel().close();
  }

}
