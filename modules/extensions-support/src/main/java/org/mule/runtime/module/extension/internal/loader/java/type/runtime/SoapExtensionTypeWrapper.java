/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Arrays.stream;
import org.mule.runtime.extension.api.annotation.soap.Soap;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class SoapExtensionTypeWrapper<T> extends TypeWrapper {

  public SoapExtensionTypeWrapper(Class<T> extensionType) {
    super(extensionType);
  }

  public List<SoapServiceProviderWrapper> getServiceProviders() {
    ImmutableList.Builder<SoapServiceProviderWrapper> result = ImmutableList.builder();

    if (SoapServiceProvider.class.isAssignableFrom(getDeclaringClass())) {
      result.add(new SoapServiceProviderWrapper(getDeclaringClass()));
    }

    getAnnotation(Soap.class)
      .ifPresent(soap -> stream(soap.value()).forEach(c -> result.add(new SoapServiceProviderWrapper(c))));
    return result.build();
  }
}
