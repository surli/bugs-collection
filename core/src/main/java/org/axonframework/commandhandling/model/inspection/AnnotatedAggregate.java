/*
 * Copyright (c) 2010-2016. Axon Framework
 *
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

package org.axonframework.commandhandling.model.inspection;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.model.Aggregate;
import org.axonframework.commandhandling.model.AggregateInvocationException;
import org.axonframework.commandhandling.model.AggregateLifecycle;
import org.axonframework.commandhandling.model.ApplyMore;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.MetaData;
import org.axonframework.messaging.annotation.MessageHandlingMember;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of the {@link Aggregate} interface that allows for an aggregate root to be a POJO with annotations on
 * its Command and Event Handler methods.
 * <p>
 * This wrapper ensures that aggregate members can use the {@link AggregateLifecycle#apply(Object)} method in a static
 * context, as long as access to the instance is done via the {@link #execute(Consumer)} or {@link #invoke(Function)}
 * methods.
 *
 * @param <T> The type of the aggregate root object
 * @see AggregateLifecycle#apply(Object)
 * @see AggregateLifecycle#markDeleted()
 */
public class AnnotatedAggregate<T> extends AggregateLifecycle implements Aggregate<T>, ApplyMore {

    private final AggregateModel<T> inspector;
    private final Queue<Runnable> delayedTasks = new LinkedList<>();
    private final EventBus eventBus;
    private T aggregateRoot;
    private boolean applying = false;
    private boolean isDeleted = false;

    /**
     * Initialize an aggregate created by the given {@code aggregateFactory} which is described in the given
     * {@code aggregateModel}. The given {@code eventBus} is used to publish events generated by the aggregate.
     *
     * @param aggregateFactory The factory to create the aggregate root instance with
     * @param aggregateModel   The model describing the aggregate structure
     * @param eventBus         The EventBus to publish events on
     * @param <T>              The type of the Aggregate root
     * @return An Aggregate instance, fully initialized
     * @throws Exception when an error occurs creating the aggregate root instance
     */
    public static <T> AnnotatedAggregate<T> initialize(Callable<T> aggregateFactory, AggregateModel<T> aggregateModel,
                                                       EventBus eventBus) throws Exception {
        AnnotatedAggregate<T> aggregate = new AnnotatedAggregate<>(aggregateModel, eventBus);
        aggregate.registerWithUnitOfWork();
        aggregate.registerRoot(aggregateFactory);
        return aggregate;
    }

    /**
     * Initialize an aggregate with the given {@code aggregateRoot} which is described in the given
     * {@code aggregateModel}. The given {@code eventBus} is used to publish events generated by the aggregate.
     *
     * @param aggregateRoot  The aggregate root instance
     * @param aggregateModel The model describing the aggregate structure
     * @param eventBus       The EventBus to publish events on
     * @param <T>            The type of the Aggregate root
     * @return An Aggregate instance, fully initialized
     */
    public static <T> AnnotatedAggregate<T> initialize(T aggregateRoot, AggregateModel<T> aggregateModel,
                                                       EventBus eventBus) {
        AnnotatedAggregate<T> aggregate = new AnnotatedAggregate<>(aggregateRoot, aggregateModel, eventBus);
        aggregate.registerWithUnitOfWork();
        return aggregate;
    }

    /**
     * Initialize an Aggregate instance for the given {@code aggregateRoot}, described by the given
     * {@code aggregateModel} that will publish events to the given {@code eventBus}.
     *
     * @param aggregateRoot The aggregate root instance
     * @param model         The model describing the aggregate structure
     * @param eventBus      The Event Bus to publish generated events on
     */
    protected AnnotatedAggregate(T aggregateRoot, AggregateModel<T> model, EventBus eventBus) {
        this.aggregateRoot = aggregateRoot;
        this.inspector = model;
        this.eventBus = eventBus;
    }

    /**
     * Initialize an Aggregate instance for the given {@code aggregateRoot}, described by the given
     * {@code aggregateModel} that will publish events to the given {@code eventBus}.
     *
     * @param inspector The AggregateModel that describes the aggregate
     * @param eventBus  The Event Bus to publish generated events on
     */
    protected AnnotatedAggregate(AggregateModel<T> inspector, EventBus eventBus) {
        this.inspector = inspector;
        this.eventBus = eventBus;
    }

    /**
     * Registers the aggregate root created by the given {@code aggregateFactory} with this aggregate. Applies any
     * delayed events that have not been applied to the aggregate yet.
     * <p>
     * This is method is commonly called while an aggregate is being initialized.
     *
     * @param aggregateFactory the factory to create the aggregate root
     * @throws Exception if the aggregate factory fails to create the aggregate root
     */
    protected void registerRoot(Callable<T> aggregateFactory) throws Exception {
        this.aggregateRoot = executeWithResult(aggregateFactory);
        execute(() -> {
            while (!delayedTasks.isEmpty()) {
                delayedTasks.poll().run();
            }
        });
    }

    @Override
    public String type() {
        return inspector.type();
    }

    @Override
    public Object identifier() {
        return inspector.getIdentifier(aggregateRoot);
    }

    @Override
    public Long version() {
        return inspector.getVersion(aggregateRoot);
    }

    @Override
    protected boolean getIsLive() {
        return true;
    }

    @Override
    public <R> R invoke(Function<T, R> invocation) {
        try {
            return executeWithResult(() -> invocation.apply(aggregateRoot));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AggregateInvocationException("Exception occurred while invoking an aggregate", e);
        }
    }

    @Override
    public void execute(Consumer<T> invocation) {
        execute(() -> invocation.accept(aggregateRoot));
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends T> rootType() {
        return (Class<? extends T>) aggregateRoot.getClass();
    }

    @Override
    protected void doMarkDeleted() {
        this.isDeleted = true;
    }

    /**
     * Publish an event to the aggregate root and its entities first and external event handlers (using the given
     * event bus) later.
     *
     * @param msg the event message to publish
     */
    protected void publish(EventMessage<?> msg) {
        inspector.publish(msg, aggregateRoot);
        publishOnEventBus(msg);
    }

    /**
     * Publish an event to external event handlers using the given event bus.
     *
     * @param msg the event message to publish
     */
    protected void publishOnEventBus(EventMessage<?> msg) {
        if (eventBus != null) {
            eventBus.publish(msg);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object handle(CommandMessage<?> msg) throws Exception {
        return executeWithResult(() -> {
            MessageHandlingMember<? super T> handler = inspector.commandHandlers().get(msg.getCommandName());
            Object result = handler.handle(msg, aggregateRoot);
            if (aggregateRoot == null) {
                aggregateRoot = (T) result;
                return identifierAsString();
            }
            return result;
        });
    }

    @Override
    protected <P> ApplyMore doApply(P payload, MetaData metaData) {
        if (!applying && aggregateRoot != null) {
            applying = true;
            try {
                publish(createMessage(payload, metaData));
                while (!delayedTasks.isEmpty()) {
                    delayedTasks.poll().run();
                }
            } finally {
                delayedTasks.clear();
                applying = false;
            }
        } else {
            delayedTasks.add(() -> publish(createMessage(payload, metaData)));
        }
        return this;
    }

    /**
     * Creates an {@link EventMessage} with given {@code payload} and {@code metaData}.
     *
     * @param payload  payload of the resulting message
     * @param metaData metadata of the resulting message
     * @param <P>      the payload type
     * @return the resulting message
     */
    protected <P> EventMessage<P> createMessage(P payload, MetaData metaData) {
        return new GenericEventMessage<>(payload, metaData);
    }

    /**
     * Get the annotated aggregate instance. Note that this method should probably never be used in normal use. If you
     * need to operate on the aggregate use {@link #invoke(Function)} or {@link #execute(Consumer)} instead.
     *
     * @return the aggregate instance
     */
    public T getAggregateRoot() {
        return aggregateRoot;
    }

    @Override
    public ApplyMore andThenApply(Supplier<?> payloadOrMessageSupplier) {
        return andThen(() -> applyMessageOrPayload(payloadOrMessageSupplier.get()));
    }

    @Override
    public ApplyMore andThen(Runnable runnable) {
        if (applying || aggregateRoot == null) {
            delayedTasks.add(runnable);
        } else {
            runnable.run();
        }
        return this;
    }

    /**
     * Apply a new event message to the aggregate and then publish this message to external systems. If the given {@code
     * payloadOrMessage} is an instance of a {@link Message} an event message is applied with the payload and metadata
     * of the given message, otherwise an event message is applied with given payload and empty metadata.
     *
     * @param payloadOrMessage defines the payload and optionally metadata to apply to the aggregate
     */
    protected void applyMessageOrPayload(Object payloadOrMessage) {
        if (payloadOrMessage instanceof Message) {
            Message message = (Message) payloadOrMessage;
            apply(message.getPayload(), message.getMetaData());
        } else if (payloadOrMessage != null) {
            apply(payloadOrMessage, MetaData.emptyInstance());
        }
    }

}
