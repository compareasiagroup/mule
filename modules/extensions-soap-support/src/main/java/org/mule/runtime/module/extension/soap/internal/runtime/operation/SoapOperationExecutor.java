/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.operation;

import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.justOrEmpty;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.services.soap.api.client.SoapClient;
import org.mule.services.soap.api.message.SoapRequest;

import java.io.ByteArrayInputStream;

import org.reactivestreams.Publisher;

/**
 * Implementation of {@link OperationExecutor} which works by using reflection to invoke a method from a class.
 *
 * @since 3.7.0
 */
public final class SoapOperationExecutor implements OperationExecutor, Startable, Stoppable {

  private OperationModel operationModel;

  public SoapOperationExecutor(OperationModel operationModel) {
    this.operationModel = operationModel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    try {
      SoapClient client = (SoapClient) executionContext.getConfiguration().get().getConnectionProvider().get().connect();
      return justOrEmpty(client.consume(buildRequest(executionContext)));
    } catch (Exception e) {
      return error(e);
    }
  }

  public SoapRequest buildRequest(ExecutionContext<OperationModel> context) {
    // Use Argument Resolver
    Object operation = context.getParameter("operation");
    String request = context.getParameter("request");
    return SoapRequest.builder().withOperation((String) operation).withContent(new ByteArrayInputStream(request.getBytes())).build();
  }

  @Override
  public void start() throws MuleException {
    //client.start();
  }

  @Override
  public void stop() throws MuleException {
    //client.stop();
  }

}
