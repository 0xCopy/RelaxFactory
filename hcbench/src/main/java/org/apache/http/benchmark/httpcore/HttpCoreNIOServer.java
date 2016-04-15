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

import org.apache.http.HttpResponseInterceptor;
import org.apache.http.benchmark.BenchConsts;
import org.apache.http.benchmark.HttpServer;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.protocol.UriHttpAsyncRequestHandlerMapper;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.protocol.*;
import org.apache.http.util.VersionInfo;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpCoreNIOServer implements HttpServer {

    private final int port;
    private final NHttpListener listener;

    public HttpCoreNIOServer(final int port) throws IOException {
        if (port <= 0) {
            throw new IllegalArgumentException("Server port may not be negative or null");
        }
        this.port = port;

        final HttpProcessor httpproc =
                new ImmutableHttpProcessor(new HttpResponseInterceptor[]{
                        new ResponseDate(), new ResponseServer("HttpCore-NIO-Test/1.1"), new ResponseContent(),
                        new ResponseConnControl()});

        final UriHttpAsyncRequestHandlerMapper registry = new UriHttpAsyncRequestHandlerMapper();
        registry.register("/rnd", new NRandomDataHandler());

        final HttpAsyncService handler =
                new HttpAsyncService(httpproc, DefaultConnectionReuseStrategy.INSTANCE,
                        DefaultHttpResponseFactory.INSTANCE, registry, null);

        final IOReactorConfig reactorConfig =
                IOReactorConfig.custom().setSoReuseAddress(true).setTcpNoDelay(BenchConsts.TCP_NO_DELAY)
                        .build();
        final ListeningIOReactor ioreactor = new DefaultListeningIOReactor(reactorConfig);
        final ConnectionConfig connectionConfig =
                ConnectionConfig.custom().setBufferSize(BenchConsts.BUF_SIZE).setFragmentSizeHint(
                        BenchConsts.BUF_SIZE).build();
        final IOEventDispatch ioEventDispatch =
                new DefaultHttpServerIODispatch(handler, connectionConfig);

        this.listener = new NHttpListener(ioreactor, ioEventDispatch);
    }

    @Override
    public String getName() {
        return "HttpCore (NIO)";
    }

    @Override
    public String getVersion() {
        final VersionInfo vinfo =
                VersionInfo.loadVersionInfo("org.apache.http", Thread.currentThread()
                        .getContextClassLoader());
        return vinfo.getRelease();
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public void start() throws Exception {
        this.listener.start();
        this.listener.listen(new InetSocketAddress(this.port));
    }

    @Override
    public void shutdown() {
        this.listener.terminate();
        try {
            this.listener.awaitTermination(1000);
        } catch (final InterruptedException ex) {
        }
        final Exception ex = this.listener.getException();
        if (ex != null) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: <port>");
            System.exit(1);
        }
        final int port = Integer.parseInt(args[0]);
        final HttpCoreNIOServer server = new HttpCoreNIOServer(port);
        System.out.println("Listening on port: " + port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                server.shutdown();
            }

        });
    }

}
