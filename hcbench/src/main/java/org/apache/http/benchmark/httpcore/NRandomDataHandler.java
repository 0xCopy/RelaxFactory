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
package org.apache.http.benchmark.httpcore;

import org.apache.http.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

class NRandomDataHandler implements HttpAsyncRequestHandler<HttpRequest> {

    public NRandomDataHandler() {
        super();
    }

    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(final HttpRequest request,
                                                                final HttpContext context) throws HttpException, IOException {
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle(final HttpRequest request, final HttpAsyncExchange httpexchange,
                       final HttpContext context) throws HttpException, IOException {
        final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
            throw new MethodNotSupportedException(method + " method not supported");
        }
        final String target = request.getRequestLine().getUri();

        int count = 100;

        final int idx = target.indexOf('?');
        if (idx != -1) {
            String s = target.substring(idx + 1);
            if (s.startsWith("c=")) {
                s = s.substring(2);
                try {
                    count = Integer.parseInt(s);
                } catch (final NumberFormatException ex) {
                    final HttpResponse response = httpexchange.getResponse();
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    response
                            .setEntity(new StringEntity("Invalid query format: " + s, ContentType.TEXT_PLAIN));
                    httpexchange.submitResponse();
                    return;
                }
            }
        }
        httpexchange.submitResponse(new RandomAsyncResponseProducer(count));
    }

    static class RandomAsyncResponseProducer implements HttpAsyncResponseProducer {

        private final ByteBuffer buf;
        private final int count;

        public RandomAsyncResponseProducer(final int count) {
            super();
            this.count = count;

            final byte[] b = new byte[count];
            final int r = Math.abs(b.hashCode());
            for (int i = 0; i < count; i++) {
                b[i] = (byte) ((r + i) % 96 + 32);
            }
            this.buf = ByteBuffer.wrap(b);
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void failed(final Exception ex) {
        }

        @Override
        public HttpResponse generateResponse() {
            final HttpResponse response =
                    new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
            final BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContentLength(this.count);
            entity.setContentType(ContentType.TEXT_PLAIN.toString());
            response.setEntity(entity);
            return response;
        }

        @Override
        public void responseCompleted(final HttpContext context) {
        }

        @Override
        public void produceContent(final ContentEncoder encoder, final IOControl ioctrl)
                throws IOException {
            while (this.buf.hasRemaining()) {
                final int bytesWritten = encoder.write(this.buf);
                if (this.buf.remaining() == 0) {
                    encoder.complete();
                }
                if (bytesWritten <= 0) {
                    break;
                }
            }
        }

    }

}
