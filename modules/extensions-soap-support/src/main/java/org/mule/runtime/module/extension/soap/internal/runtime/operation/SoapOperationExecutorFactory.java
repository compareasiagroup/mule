/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.operation;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;

/**
 * Implementation of {@link OperationExecutor} which works by using reflection to invoke a method from a class.
 *
 * @since 3.7.0
 */
public final class SoapOperationExecutorFactory implements OperationExecutorFactory {

  //private SoapClient client;
  //
  //public SoapOperationExecutor(SoapClient client) {
    //this.client = client;
  //}

  @Override
  public OperationExecutor createExecutor(OperationModel operationModel) {
    return new SoapOperationExecutor(operationModel);
  }
}
