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

package org.apache.hc.core5.testing.nio;

import javax.net.ssl.SSLContext;

import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.ConnectionReuseStrategy;
import org.apache.hc.core5.http.ContentLengthStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.config.CharCodingConfig;
import org.apache.hc.core5.http.config.H1Config;
import org.apache.hc.core5.http.impl.ConnectionListener;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import org.apache.hc.core5.http.impl.DefaultContentLengthStrategy;
import org.apache.hc.core5.http.impl.Http1StreamListener;
import org.apache.hc.core5.http.impl.nio.ClientHttp1IOEventHandler;
import org.apache.hc.core5.http.impl.nio.ClientHttp1StreamDuplexer;
import org.apache.hc.core5.http.impl.nio.DefaultHttpRequestWriterFactory;
import org.apache.hc.core5.http.impl.nio.DefaultHttpResponseParserFactory;
import org.apache.hc.core5.http.nio.NHttpMessageParser;
import org.apache.hc.core5.http.nio.NHttpMessageParserFactory;
import org.apache.hc.core5.http.nio.NHttpMessageWriter;
import org.apache.hc.core5.http.nio.NHttpMessageWriterFactory;
import org.apache.hc.core5.http.protocol.HttpProcessor;
import org.apache.hc.core5.reactor.IOEventHandler;
import org.apache.hc.core5.reactor.IOEventHandlerFactory;
import org.apache.hc.core5.reactor.TlsCapableIOSession;
import org.apache.hc.core5.util.Args;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @since 5.0
 */
@Contract(threading = ThreadingBehavior.IMMUTABLE)
class InternalClientHttp1EventHandlerFactory implements IOEventHandlerFactory {

    private final HttpProcessor httpProcessor;
    private final H1Config h1Config;
    private final CharCodingConfig charCodingConfig;
    private final ConnectionReuseStrategy connectionReuseStrategy;
    private final SSLContext sslContext;
    private final NHttpMessageParserFactory<HttpResponse> responseParserFactory;
    private final NHttpMessageWriterFactory<HttpRequest> requestWriterFactory;

    InternalClientHttp1EventHandlerFactory(
            final HttpProcessor httpProcessor,
            final H1Config h1Config,
            final CharCodingConfig charCodingConfig,
            final ConnectionReuseStrategy connectionReuseStrategy,
            final SSLContext sslContext) {
        this.httpProcessor = Args.notNull(httpProcessor, "HTTP processor");
        this.h1Config = h1Config != null ? h1Config : H1Config.DEFAULT;
        this.charCodingConfig = charCodingConfig != null ? charCodingConfig : CharCodingConfig.DEFAULT;
        this.connectionReuseStrategy = connectionReuseStrategy != null ? connectionReuseStrategy :
                DefaultConnectionReuseStrategy.INSTANCE;
        this.sslContext = sslContext;
        this.responseParserFactory = new DefaultHttpResponseParserFactory(this.h1Config);
        this.requestWriterFactory = DefaultHttpRequestWriterFactory.INSTANCE;
    }

    protected ClientHttp1StreamDuplexer createClientHttp1StreamDuplexer(
            final TlsCapableIOSession ioSession,
            final HttpProcessor httpProcessor,
            final H1Config h1Config,
            final CharCodingConfig charCodingConfig,
            final ConnectionReuseStrategy connectionReuseStrategy,
            final NHttpMessageParser<HttpResponse> incomingMessageParser,
            final NHttpMessageWriter<HttpRequest> outgoingMessageWriter,
            final ContentLengthStrategy incomingContentStrategy,
            final ContentLengthStrategy outgoingContentStrategy,
            final ConnectionListener connectionListener,
            final Http1StreamListener streamListener) {
        return new ClientHttp1StreamDuplexer(ioSession, httpProcessor, h1Config, charCodingConfig,
                connectionReuseStrategy, incomingMessageParser, outgoingMessageWriter,
                incomingContentStrategy, outgoingContentStrategy,
                connectionListener, streamListener);
    }

    @Override
    public IOEventHandler createHandler(final TlsCapableIOSession ioSession, final Object attachment) {
        final String id = ioSession.getId();
        if (sslContext != null) {
            ioSession.startTls(sslContext, null ,null, null);
        }
        final Logger sessionLog = LogManager.getLogger(ioSession.getClass());
        final Logger wireLog = LogManager.getLogger("org.apache.hc.core5.http.wire");
        final ClientHttp1StreamDuplexer streamDuplexer = createClientHttp1StreamDuplexer(
                new LoggingIOSession(ioSession, sessionLog, wireLog),
                httpProcessor,
                h1Config,
                charCodingConfig,
                connectionReuseStrategy,
                responseParserFactory.create(),
                requestWriterFactory.create(),
                DefaultContentLengthStrategy.INSTANCE,
                DefaultContentLengthStrategy.INSTANCE,
                new InternalConnectionListener(id, sessionLog),
                new InternalHttp1StreamListener(id, InternalHttp1StreamListener.Type.CLIENT, sessionLog));
        return new LoggingIOEventHandler(new ClientHttp1IOEventHandler(streamDuplexer), id, sessionLog);
    }

}
