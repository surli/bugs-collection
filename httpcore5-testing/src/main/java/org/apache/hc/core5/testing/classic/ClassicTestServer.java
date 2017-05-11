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

package org.apache.hc.core5.testing.classic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;

import org.apache.hc.core5.http.ConnectionClosedException;
import org.apache.hc.core5.http.ExceptionListener;
import org.apache.hc.core5.http.config.H1Config;
import org.apache.hc.core5.http.config.SocketConfig;
import org.apache.hc.core5.http.impl.bootstrap.HttpServer;
import org.apache.hc.core5.http.impl.bootstrap.ServerBootstrap;
import org.apache.hc.core5.http.io.HttpConnectionFactory;
import org.apache.hc.core5.http.io.HttpExpectationVerifier;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.UriHttpRequestHandlerMapper;
import org.apache.hc.core5.http.protocol.HttpProcessor;
import org.apache.hc.core5.io.ShutdownType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class ClassicTestServer {

    private final SSLContext sslContext;
    private final SocketConfig socketConfig;
    private final UriHttpRequestHandlerMapper registry;

    private final AtomicReference<HttpServer> serverRef;

    public ClassicTestServer(final SSLContext sslContext, final SocketConfig socketConfig) {
        super();
        this.sslContext = sslContext;
        this.socketConfig = socketConfig != null ? socketConfig : SocketConfig.DEFAULT;
        this.registry = new UriHttpRequestHandlerMapper();
        this.serverRef = new AtomicReference<>(null);
    }

    public ClassicTestServer(final SocketConfig socketConfig) {
        this(null, socketConfig);
    }

    public ClassicTestServer() {
        this(null, null);
    }

    public void registerHandler(
            final String pattern,
            final HttpRequestHandler handler) {
        this.registry.register(pattern, handler);
    }

    public int getPort() {
        final HttpServer server = this.serverRef.get();
        if (server != null) {
            return server.getLocalPort();
        } else {
            throw new IllegalStateException("Server not running");
        }
    }

    public InetAddress getInetAddress() {
        final HttpServer server = this.serverRef.get();
        if (server != null) {
            return server.getInetAddress();
        } else {
            throw new IllegalStateException("Server not running");
        }
    }

    public void start(final HttpProcessor httpProcessor, final HttpExpectationVerifier expectationVerifier) throws IOException {
        if (serverRef.get() == null) {
            final HttpServer server = ServerBootstrap.bootstrap()
                    .setSocketConfig(socketConfig)
                    .setSslContext(sslContext)
                    .setHttpProcessor(httpProcessor)
                    .setExpectationVerifier(expectationVerifier)
                    .setHandlerMapper(this.registry)
                    .setConnectionFactory(new LoggingConnFactory())
                    .setExceptionListener(new SimpleExceptionListener())
                    .create();
            if (serverRef.compareAndSet(null, server)) {
                server.start();
            }
        } else {
            throw new IllegalStateException("Server already running");
        }
    }

    public void start(final HttpExpectationVerifier expectationVerifier) throws IOException {
        start(null, expectationVerifier);
    }

    public void start() throws IOException {
        start(null, null);
    }

    public void shutdown(final ShutdownType shutdownType) {
        final HttpServer server = serverRef.getAndSet(null);
        if (server != null) {
            server.shutdown(shutdownType);
        }
    }

    class LoggingConnFactory implements HttpConnectionFactory<LoggingBHttpServerConnection> {

        @Override
        public LoggingBHttpServerConnection createConnection(final Socket socket) throws IOException {
            final LoggingBHttpServerConnection conn = new LoggingBHttpServerConnection(H1Config.DEFAULT);
            conn.bind(socket);
            return conn;
        }
    }

    static class SimpleExceptionListener implements ExceptionListener {

        private final Logger log = LogManager.getLogger(ClassicTestServer.class);

        @Override
        public void onError(final Exception ex) {
            if (ex instanceof ConnectionClosedException) {
                this.log.debug(ex.getMessage());
            } else if (ex instanceof SocketException) {
                this.log.debug(ex.getMessage());
            } else {
                this.log.error(ex.getMessage(), ex);
            }
        }
    }

}
