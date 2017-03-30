/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import org.mule.runtime.module.extension.internal.loader.java.type.ParameterizableTypeElement;


public class SoapServiceProviderWrapper extends SoapComponentWrapper implements ParameterizableTypeElement {

  SoapServiceProviderWrapper(Class aClass) {
    super(aClass);
  }

}
