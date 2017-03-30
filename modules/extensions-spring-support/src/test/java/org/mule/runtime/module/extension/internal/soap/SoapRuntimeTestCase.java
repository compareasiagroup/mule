/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.soap;

import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionModelLoader;
import org.mule.test.soap.extension.SoapConnectExtension;

import org.junit.Test;

public class SoapRuntimeTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "soap-config.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {SoapConnectExtension.class};
  }

  @Override
  protected ExtensionModelLoader getExtensionModelLoader() {
    return new SoapExtensionModelLoader();
  }

  @Test
  public void run() throws Exception {
    Event runSoap = flowRunner("runSoap")
      .withPayload("<con:echo xmlns:con=\"http://service.soap.services.mule.org/\">"
      + "    <text>test</text>\n"
      + "</con:echo>")
      .run();
    int i = 0;
  }
}
