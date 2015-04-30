/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.http.benchmark.jetty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

class RandomDataHandler extends AbstractHandler {

    public RandomDataHandler() {
        super();
    }

    @Override
    public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException, ServletException {
        if (target.equals("/rnd")) {
            rnd(request, response);
        } else {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            response.setContentType("text/plain");
            final Writer writer = response.getWriter();
            writer.write("Target not found: " + target);
            writer.flush();
        }
    }

    private void rnd(
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        int count = 100;
        final String s = request.getParameter("c");
        try {
            count = Integer.parseInt(s);
        } catch (final NumberFormatException ex) {
            response.setStatus(500);
            final Writer writer = response.getWriter();
            writer.write("Invalid query format: " + request.getQueryString());
            writer.flush();
            return;
        }

        response.setStatus(200);
        response.setContentType("text/plain");
        response.setContentLength(count);

        final byte[] b = new byte[count];
        final int r = Math.abs(b.hashCode());
        for (int i = 0; i < count; i++) {
            b[i] = (byte) ((r + i) % 96 + 32);
        }
        
        final OutputStream outstream = response.getOutputStream();
        outstream.write(b);
        outstream.flush();
    }

}
