/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.core.util.ClassUtils.loadClass;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

/**
 * Loads an extension by introspecting a class which uses the Extensions API annotations
 *
 * @since 4.0
 */
public class SoapExtensionModelLoader extends ExtensionModelLoader {

  public static final String LOADER_ID = "soap";
  public static final String TYPE_PROPERTY_NAME = "type";
  public static final String VERSION = "version";

  //private final List<ExtensionModelValidator> customValidators = unmodifiableList(asList(
  //                                                                                       new ConfigurationModelValidator(),
  //                                                                                       new ConnectionProviderModelValidator(),
  //                                                                                       new ExportedTypesModelValidator(),
  //                                                                                       new JavaSubtypesModelValidator(),
  //                                                                                       new MetadataComponentModelValidator(),
  //                                                                                       new NullSafeModelValidator(),
  //                                                                                       new OperationReturnTypeModelValidator(),
  //                                                                                       new OperationParametersTypeModelValidator(),
  //                                                                                       new ParameterGroupModelValidator(),
  //                                                                                       new ParameterTypeModelValidator()));
  //private final List<DeclarationEnricher> customDeclarationEnrichers = unmodifiableList(asList(
  //                                                                                             new ClassLoaderDeclarationEnricher(),
  //                                                                                             new JavaXmlDeclarationEnricher(),
  //                                                                                             new ConfigNameDeclarationEnricher(),
  //                                                                                             new ConnectionDeclarationEnricher(),
  //                                                                                             new ErrorsDeclarationEnricher(),
  //                                                                                             new ConnectionErrorsDeclarationEnricher(),
  //                                                                                             new DataTypeDeclarationEnricher(),
  //                                                                                             new DisplayDeclarationEnricher(),
  //                                                                                             new DynamicMetadataDeclarationEnricher(),
  //                                                                                             new ImportedTypesDeclarationEnricher(),
  //                                                                                             new JavaConfigurationDeclarationEnricher(),
  //                                                                                             new JavaExportedTypesDeclarationEnricher(),
  //                                                                                             new SubTypesDeclarationEnricher(),
  //                                                                                             new ExtensionDescriptionsEnricher(),
  //                                                                                             new ParameterLayoutOrderDeclarationEnricher()));

  /**
   * {@inheritDoc}
   * @return {@link #LOADER_ID}
   */
  @Override
  public String getId() {
    return LOADER_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    //context.addCustomValidators(customValidators);
    //context.addCustomDeclarationEnrichers(customDeclarationEnrichers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    Class<?> extensionType = getExtensionType(context);
    String version =
        context.<String>getParameter(VERSION).orElseThrow(() -> new IllegalArgumentException("version not specified"));
    new SoapModelLoaderDelegate(extensionType, version).declare(context);
  }

  private Class<?> getExtensionType(ExtensionLoadingContext context) {
    String type = context.<String>getParameter(TYPE_PROPERTY_NAME).get();
    if (isBlank(type)) {
      throw new IllegalArgumentException(format("Property '%s' has not been specified", TYPE_PROPERTY_NAME));
    }

    try {
      return loadClass(type, context.getExtensionClassLoader());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(format("Class '%s' cannot be loaded '%s'", type), e);
    }
  }
}
