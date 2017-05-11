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

package org.apache.hc.core5.http.io;

import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.LookupRegistry;
import org.apache.hc.core5.http.protocol.UriPatternMatcher;
import org.apache.hc.core5.util.Args;

/**
 * Maintains a map of HTTP request handlers keyed by a request URI pattern.
 * <br>
 * Patterns may have three formats:
 * <ul>
 * <li>{@code *}</li>
 * <li>{@code *<uri>}</li>
 * <li>{@code <uri>*}</li>
 * </ul>
 * <br>
 * This class can be used to map an instance of
 * {@link HttpRequestHandler} matching a particular request URI. Usually the
 * mapped request handler will be used to process the request with the
 * specified request URI.
 *
 * @since 4.3
 */
@Contract(threading = ThreadingBehavior.SAFE)
public class UriHttpRequestHandlerMapper implements HttpRequestHandlerMapper {

    private final LookupRegistry<HttpRequestHandler> matcher;

    public UriHttpRequestHandlerMapper(final LookupRegistry<HttpRequestHandler> matcher) {
        super();
        this.matcher = Args.notNull(matcher, "Pattern matcher");
    }

    public UriHttpRequestHandlerMapper() {
        this(new UriPatternMatcher<HttpRequestHandler>());
    }

    /**
     * Registers the given {@link HttpRequestHandler} as a handler for URIs
     * matching the given pattern.
     *
     * @param pattern the pattern to register the handler for.
     * @param handler the handler.
     */
    public void register(final String pattern, final HttpRequestHandler handler) {
        Args.notNull(pattern, "Pattern");
        Args.notNull(handler, "Handler");
        matcher.register(pattern, handler);
    }

    /**
     * Removes registered handler, if exists, for the given pattern.
     *
     * @param pattern the pattern to unregister the handler for.
     */
    public void unregister(final String pattern) {
        matcher.unregister(pattern);
    }

    /**
     * Extracts request path from the given {@link HttpRequest}
     */
    protected String getRequestPath(final HttpRequest request) {
        String uriPath = request.getPath();
        int index = uriPath.indexOf("?");
        if (index != -1) {
            uriPath = uriPath.substring(0, index);
        } else {
            index = uriPath.indexOf("#");
            if (index != -1) {
                uriPath = uriPath.substring(0, index);
            }
        }
        return uriPath;
    }

    /**
     * Looks up a handler matching the given request URI.
     *
     * @param request the request
     * @return handler or {@code null} if no match is found.
     */
    @Override
    public HttpRequestHandler lookup(final HttpRequest request, final HttpContext context) {
        Args.notNull(request, "HTTP request");
        return matcher.lookup(getRequestPath(request));
    }

}
