/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.soap.extension;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.soap.Soap;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Soap
@Extension(name = "soap", description = "Soap Connect Test Extension")
public class SoapConnectExtension implements SoapServiceProvider {


  @Parameter
  private String uno;

  @Parameter
  private String dos;


  @Override
  public List<WebServiceDefinition> getWebServiceDefinitions() {
    URL url = null;
    try {
      url = new URL("http://wsdl.com");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return singletonList(new WebServiceDefinition("OnlyService", url, "service", "port", emptyList()));
  }

  @Override
  public URL getServiceAddress(WebServiceDefinition definition) {
    try {
      return new URL("http://someaddress.com");
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
