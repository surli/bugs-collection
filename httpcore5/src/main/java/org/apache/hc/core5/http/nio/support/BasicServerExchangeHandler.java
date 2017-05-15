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
package org.apache.hc.core5.http.nio.support;

import java.io.IOException;

import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Args;

/**
 * @since 5.0
 */
public class BasicServerExchangeHandler<T> extends AbstractServerExchangeHandler<T> {

    private final RequestConsumerSupplier<T> consumerSupplier;
    private final ResponseHandler<T> responseHandler;

    public BasicServerExchangeHandler(
            final RequestConsumerSupplier<T> consumerSupplier,
            final ResponseHandler<T> responseHandler) {
        super();
        this.consumerSupplier = Args.notNull(consumerSupplier, "Consumer supplier");
        this.responseHandler = Args.notNull(responseHandler, "Response handler");
    }

    @Override
    protected AsyncRequestConsumer<T> supplyConsumer(
            final HttpRequest request,
            final HttpContext context) throws HttpException {
        return consumerSupplier.get(request, context);
    }

    @Override
    protected void handle(
            final T requestMessage,
            final ResponseTrigger responseTrigger,
            final HttpContext context) throws HttpException, IOException {
        responseHandler.handle(requestMessage, responseTrigger, context);
    }

}
