/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.api.soap.SoapTransportProvider;
import org.mule.services.soap.api.client.DispatcherResponse;
import org.mule.services.soap.api.client.MessageDispatcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

public class SoapConnectionProviderFactory implements ConnectionProviderFactory {


  public SoapConnectionProviderFactory(Class<?> transportProvider, ClassLoader cl) {
  }

  @Override
  public ConnectionProvider newInstance() {
    return new SoapConnectionProvider(new SoapTransportProvider() {

      @Override
      public MessageDispatcher connect() throws ConnectionException {
        return new MessageDispatcher() {

          @Override
          public DispatcherResponse dispatch(InputStream message, Map<String, String> properties) {
            return new DispatcherResponse("", new ByteArrayInputStream("".getBytes()));
          }

          @Override
          public void dispose() {

          }

          @Override
          public void initialise() throws InitialisationException {

          }
        };
      }

      @Override
      public void disconnect(MessageDispatcher connection) {

      }

      @Override
      public ConnectionValidationResult validate(MessageDispatcher connection) {
        return ConnectionValidationResult.success();
      }
    });
  }

  @Override
  public Class<? extends ConnectionProvider> getObjectType() {
    return SoapConnectionProvider.class;
  }
}
