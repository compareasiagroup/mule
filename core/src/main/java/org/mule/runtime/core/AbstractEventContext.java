/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.stream.Collectors.toList;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.when;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;

import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

/**
 * Base class for implementations of {@link EventContext}
 *
 * @since 4.0
 */
abstract class AbstractEventContext implements EventContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventContext.class);

  private transient MonoProcessor<Event> responseProcessor;
  private transient MonoProcessor<Void> completionProcessor;
  private transient Disposable completionSubscriberDisposable;
  private final List<EventContext> childContexts = new LinkedList<>();
  private transient Mono<Void> completionCallback = empty();

  public AbstractEventContext() {
    this(empty());
  }

  public AbstractEventContext(Publisher<Void> completionCallback) {
    this.completionCallback = from(completionCallback);
    initCompletionProcessor();
  }

  private void initCompletionProcessor() {
    responseProcessor = MonoProcessor.create();
    completionProcessor = MonoProcessor.create();
    completionProcessor.doFinally(e -> LOGGER.debug(this + " execution completed.")).subscribe();
    // When there are no child contexts response triggers completion directly.
    completionSubscriberDisposable = Mono.<Void>whenDelayError(completionCallback,
                                                               responseProcessor.then())
        .doOnEach(s -> s.accept(completionProcessor)).subscribe();
  }

  void addChildContext(EventContext childContext) {
    synchronized (this) {
      childContexts.add(childContext);
      // When a new child is added dispose existing subscription that triggers completion processor and re-subscribe adding child
      // completion condition.
      completionSubscriberDisposable.dispose();
      completionSubscriberDisposable =
          responseProcessor.otherwise(throwable -> empty()).and(completionCallback).and(getChildCompletionPublisher()).then()
              .doOnEach(s -> s.accept(completionProcessor)).subscribe();
    }
  }

  private Mono<Void> getChildCompletionPublisher() {
    return when(childContexts.stream()
        .map(eventContext -> from(eventContext.getCompletionPublisher()).otherwise(throwable -> empty()))
        .collect(toList()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success() {
    synchronized (this) {
      LOGGER.debug(this + " response completed with no result.");
      responseProcessor.onComplete();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success(Event event) {
    synchronized (this) {
      LOGGER.debug(this + " response completed with result.");
      responseProcessor.onNext(event);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void error(Throwable throwable) {
    synchronized (this) {
      LOGGER.debug(this + " response completed with error.");
      responseProcessor.onError(throwable);
    }
  }

  @Override
  public Publisher<Event> getResponsePublisher() {
    return responseProcessor;
  }

  @Override
  public Publisher<Void> getCompletionPublisher() {
    return completionProcessor;
  }

  private void readObject(ObjectInputStream in) throws Exception {
    in.defaultReadObject();
    initCompletionProcessor();
  }

}
