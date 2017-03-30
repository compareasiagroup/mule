/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.util.IOUtils.getResourceAsString;
import static org.mule.runtime.module.extension.internal.loader.java.JavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.JavaExtensionModelLoader.VERSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.module.extension.internal.capability.xml.schema.SchemaGenerator;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaXmlDeclarationEnricher;
import org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.soap.extension.SoapConnectExtension;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class SoapExtensionSchemaGeneratorTestCase extends AbstractMuleTestCase {

  static final Map<String, ExtensionModel> extensionModels = new HashMap<>();

  @Parameterized.Parameter(0)
  public ExtensionModel extensionUnderTest;

  @Parameterized.Parameter(1)
  public String expectedXSD;

  private SchemaGenerator generator;
  private String expectedSchema;


  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    final ClassLoader classLoader = SoapExtensionSchemaGeneratorTestCase.class.getClassLoader();
    final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
    when(serviceRegistry.lookupProviders(DeclarationEnricher.class, classLoader))
        .thenReturn(asList(new JavaXmlDeclarationEnricher()));

    final Map<Class<?>, String> extensions = new LinkedHashMap<Class<?>, String>() {

      {
        put(SoapConnectExtension.class, "soap.xsd");
      }
    };

    Function<Class<?>, ExtensionModel> createExtensionModel = extension -> {
      ExtensionModel model = loadSoapExtension(extension, new HashMap<>());

      if (extensionModels.put(model.getName(), model) != null) {
        throw new IllegalArgumentException(format("Extension names must be unique. Name [%s] for extension [%s] was already used",
                                                  model.getName(), extension.getName()));
      }

      return model;
    };

    return extensions.entrySet().stream()
        .map(e -> new Object[] {createExtensionModel.apply(e.getKey()), e.getValue()})
        .collect(toList());
  }

  @Before
  public void setup() throws IOException {
    generator = new SchemaGenerator();
    expectedSchema = getResourceAsString("schemas/" + expectedXSD, getClass());
  }

  @Test
  public void generate() throws Exception {
    XmlDslModel languageModel = extensionUnderTest.getXmlDslModel();
    String schema = generator.generate(extensionUnderTest, languageModel, new SchemaTestDslContext());
    compareXML(expectedSchema, schema);
  }

  private static class SchemaTestDslContext implements DslResolvingContext {

    @Override
    public Optional<ExtensionModel> getExtension(String name) {
      return ofNullable(extensionModels.get(name));
    }

    @Override
    public Set<ExtensionModel> getExtensions() {
      return copyOf(extensionModels.values());
    }

    @Override
    public TypeCatalog getTypeCatalog() {
      return TypeCatalog.getDefault(copyOf(extensionModels.values()));
    }
  }

  public static ExtensionModel loadSoapExtension(Class<?> clazz, Map<String, Object> params) {
    params.put(TYPE_PROPERTY_NAME, clazz.getName());
    params.put(VERSION, getProductVersion());
    return new SoapExtensionModelLoader().loadExtensionModel(clazz.getClassLoader(), params);
  }
}
