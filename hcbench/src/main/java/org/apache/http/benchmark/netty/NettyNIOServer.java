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

package org.apache.http.benchmark.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.http.benchmark.BenchConsts;
import org.apache.http.benchmark.HttpServer;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class NettyNIOServer implements HttpServer {

    private final int port;
    private final ServerBootstrap serverBootstrap;

    public NettyNIOServer(final int port) {
        super();
        if (port <= 0) {
            throw new IllegalArgumentException("Server port may not be negative or null");
        }
        this.port = port;
        this.serverBootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));
        this.serverBootstrap.setPipelineFactory(new HttpServerPipelineFactory());
        this.serverBootstrap.setOption("child.tcpNoDelay", Boolean.valueOf(BenchConsts.TCP_NO_DELAY));
    }

    @Override
    public String getName() {
        return "Netty";
    }

    @Override
    public String getVersion() {
        return "3.6.2";
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public void start() throws Exception {
        serverBootstrap.bind(new InetSocketAddress(port));
    }

    @Override
    public void shutdown() {
        serverBootstrap.releaseExternalResources();
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: <port>");
            System.exit(1);
        }
        final int port = Integer.parseInt(args[0]);
        final NettyNIOServer server = new NettyNIOServer(port);
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
