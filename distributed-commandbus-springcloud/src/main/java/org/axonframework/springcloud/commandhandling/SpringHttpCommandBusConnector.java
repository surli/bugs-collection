/*
 * Copyright (c) 2010-2017. Axon Framework
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.springcloud.commandhandling;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.distributed.CommandBusConnector;
import org.axonframework.commandhandling.distributed.Member;
import org.axonframework.common.Registration;
import org.axonframework.messaging.MessageHandler;
import org.axonframework.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestOperations;

@RestController
@RequestMapping("/spring-command-bus-connector")
public class SpringHttpCommandBusConnector implements CommandBusConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringHttpCommandBusConnector.class);

    private static final boolean EXPECT_REPLY = true;
    private static final boolean DO_NOT_EXPECT_REPLY = false;
    private static final String COMMAND_BUS_CONNECTOR_PATH = "/spring-command-bus-connector/command";

    private final CommandBus localCommandBus;
    private final RestOperations restOperations;
    private final Serializer serializer;

    public SpringHttpCommandBusConnector(CommandBus localCommandBus, RestOperations restOperations, Serializer serializer) {
        this.localCommandBus = localCommandBus;
        this.restOperations = restOperations;
        this.serializer = serializer;
    }

    @Override
    public <C> void send(Member destination, CommandMessage<? extends C> commandMessage) throws Exception {
        if (destination.local()) {
            localCommandBus.dispatch(commandMessage);
        } else {
            sendRemotely(destination, commandMessage, DO_NOT_EXPECT_REPLY);
        }
    }

    @Override
    public <C, R> void send(Member destination, CommandMessage<C> commandMessage,
                            CommandCallback<? super C, R> callback) throws Exception {
        if (destination.local()) {
            localCommandBus.dispatch(commandMessage, callback);
        } else {
            SpringHttpReplyMessage<R> replyMessage = this.<C, R>sendRemotely(destination, commandMessage, EXPECT_REPLY).getBody();
            if (replyMessage.isSuccess()) {
                callback.onSuccess(commandMessage, replyMessage.getReturnValue(serializer));
            } else {
                callback.onFailure(commandMessage, replyMessage.getError(serializer));
            }
        }
    }

    /**
     * Send the command message to a remote member
     *
     * @param destination    The member of the network to send the message to
     * @param commandMessage The command to send to the (remote) member
     * @param expectReply    True if a reply is expected
     * @param <C>            The type of object expected as command
     * @param <R>            The type of object expected as result of the command
     * @return The reply
     */
    private <C, R> ResponseEntity<SpringHttpReplyMessage<R>> sendRemotely(Member destination,
                                                                          CommandMessage<? extends C> commandMessage,
                                                                          boolean expectReply) {
        Optional<URI> optionalEndpoint = destination.getConnectionEndpoint(URI.class);
        if (optionalEndpoint.isPresent()) {
            URI endpointUri = optionalEndpoint.get();
            URI destinationUri = buildURIForPath(endpointUri.getScheme(), endpointUri.getUserInfo(),
                    endpointUri.getHost(), endpointUri.getPort(), endpointUri.getPath());

            SpringHttpDispatchMessage<C> dispatchMessage =
                    new SpringHttpDispatchMessage<>(commandMessage, serializer, expectReply);
            return restOperations.exchange(destinationUri, HttpMethod.POST, new HttpEntity<>(dispatchMessage),
                    new ParameterizedTypeReference<SpringHttpReplyMessage<R>>(){});
        } else {
            String errorMessage = String.format("No Connection Endpoint found in Member [%s] for protocol [%s] " +
                    "to send the command message [%s] to", destination, URI.class, commandMessage);
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private URI buildURIForPath(String scheme, String userInfo, String host, int port, String path) {
        try {
            return new URI(scheme, userInfo, host, port, path + COMMAND_BUS_CONNECTOR_PATH, null, null);
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to build URI for [{}{}{}], with user info [{}] and path [{}]",
                    scheme, host, port, userInfo, COMMAND_BUS_CONNECTOR_PATH, e);
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Registration subscribe(String commandName, MessageHandler<? super CommandMessage<?>> handler) {
        return localCommandBus.subscribe(commandName, handler);
    }

    @PostMapping("/command")
    public <C, R> CompletableFuture<?> receiveCommand(
            @RequestBody SpringHttpDispatchMessage<C> dispatchMessage) throws ExecutionException, InterruptedException {
        CommandMessage<C> commandMessage = dispatchMessage.getCommandMessage(serializer);
        if (dispatchMessage.isExpectReply()) {
            try {
                SpringHttpReplyFutureCallback<C, R> replyFutureCallback = new SpringHttpReplyFutureCallback<>();
                localCommandBus.dispatch(commandMessage, replyFutureCallback);
                return replyFutureCallback;
            } catch (Exception e) {
                LOGGER.error("Could not dispatch command", e);
                return CompletableFuture.completedFuture(createReply(commandMessage, false, e));
            }
        } else {
            try {
                localCommandBus.dispatch(commandMessage);
                return CompletableFuture.completedFuture("");
            } catch (Exception e) {
                LOGGER.error("Could not dispatch command", e);
                return CompletableFuture.completedFuture(createReply(commandMessage, false, e));
            }
        }
    }

    private SpringHttpReplyMessage createReply(CommandMessage<?> commandMessage, boolean success, Object result) {
        try {
            return new SpringHttpReplyMessage<>(commandMessage.getIdentifier(), success, result, serializer);
        } catch (Exception e) {
            LOGGER.warn("Could not serialize command reply [{}]. Sending back NULL.", result, e);
            return new SpringHttpReplyMessage(commandMessage.getIdentifier(), success, null, serializer);
        }
    }

    public class SpringHttpReplyFutureCallback<C, R> extends CompletableFuture<SpringHttpReplyMessage>
            implements CommandCallback<C, R> {

        @Override
        public void onSuccess(CommandMessage<? extends C> commandMessage, R result) {
            super.complete(createReply(commandMessage, true, result));
        }

        @Override
        public void onFailure(CommandMessage commandMessage, Throwable cause) {
            super.complete(createReply(commandMessage, false, cause));
        }

    }

}
