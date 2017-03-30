/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static org.mule.services.soap.api.client.SoapClientConfiguration.builder;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.soap.SoapTransportProvider;
import org.mule.services.soap.api.SoapService;
import org.mule.services.soap.api.client.MessageDispatcher;
import org.mule.services.soap.api.client.SoapClient;

import javax.inject.Inject;

public class SoapConnectionProvider implements ConnectionProvider<SoapClient> {

  @Inject
  private SoapService service;

  private SoapTransportProvider transportProvider;

  public SoapConnectionProvider(SoapTransportProvider transportProvider) {
    this.transportProvider = transportProvider;
  }

  @Override
  public SoapClient connect() throws ConnectionException {
    MessageDispatcher dispatcher = transportProvider.connect();

    try {
      dispatcher.initialise();
    } catch (InitialisationException e) {
      e.printStackTrace();
    }

    return service.getClientFactory()
      .create(
        builder().withPort("GlobalWeatherSoap").withService("GlobalWeather").withWsdlLocation("http://www.webservicex.com/globalweather.asmx?WSDL")
          .withCustomDispatcher(dispatcher).build());
  }

  @Override
  public void disconnect(SoapClient connection) {
    try {
      connection.stop();
    } catch (MuleException e) {
      e.printStackTrace();
    }
  }

  @Override
  public ConnectionValidationResult validate(SoapClient connection) {
    return ConnectionValidationResult.success();
  }
}
