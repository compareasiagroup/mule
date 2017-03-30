/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader.type.runtime;

import org.mule.runtime.module.extension.internal.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

import java.lang.reflect.Type;
import java.util.List;

public class SoapConnectionProviderTypeWrapper extends TypeWrapper implements ConnectionProviderElement {

  public SoapConnectionProviderTypeWrapper(Class<?> clazz) {
    super(clazz);
  }

  @Override
  public List<Type> getSuperClassGenerics() {
    return null;
  }

  @Override
  public List<Class<?>> getInterfaceGenerics(Class clazz) {
    return null;
  }
}
