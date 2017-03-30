/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.loader.java.JavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.JavaExtensionModelLoader.VERSION;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.test.soap.extension.SoapConnectExtension;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SoapExtensionLoaderTestCase {

  SoapExtensionModelLoader loader = new SoapExtensionModelLoader();

  @Test
  public void loadSoapExtension() {
    Map<String, Object> params = new HashMap<>();
    params.put(TYPE_PROPERTY_NAME, SoapConnectExtension.class.getName());
    params.put(VERSION, getProductVersion());
    ExtensionModel model = loader.loadExtensionModel(SoapConnectExtension.class.getClassLoader(), params);
    //extensionManager.registerExtension(model);

    String name = model.getName();
  }

}
