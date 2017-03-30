/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.roleOf;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentDeclarationTypeName;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DEFAULT_CONNECTION_PROVIDER_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.parseLayoutAnnotations;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExpressionSupport;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.BasicTypeMetadataVisitor;
import org.mule.runtime.api.message.NullAttributes;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.Stereotype;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.RestrictedTo;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.DefaultEncoding;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser;
import org.mule.runtime.module.extension.internal.loader.java.ParameterResolverTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.TypeAwareConfigurationFactory;
import org.mule.runtime.module.extension.internal.loader.java.TypedValueTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.contributor.InfrastructureFieldContributor;
import org.mule.runtime.module.extension.internal.loader.java.contributor.ParameterDeclarerContributor;
import org.mule.runtime.module.extension.internal.loader.java.contributor.ParameterTypeUnwrapperContributor;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DefaultEncodingModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.TypeRestrictionModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionTypeFactory;
import org.mule.runtime.module.extension.internal.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.FieldWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.SoapExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.SoapServiceProviderWrapper;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.SoapConnectionProviderFactory;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.SoapConnectionProvider;
import org.mule.runtime.module.extension.soap.internal.runtime.operation.SoapOperationExecutorFactory;
import org.mule.services.soap.api.client.SoapClient;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Describes an {@link ExtensionModel} by analyzing the annotations in the class
 * provided in the constructor
 *
 * @since 4.0
 */
final class SoapModelLoaderDelegate {

  private final Class<?> extensionType;
  private final ClassTypeLoader typeLoader;
  private final String version;

  private final Map<Class<?>, ConnectionProviderDeclarer> connectionProviderDeclarers = new HashMap<>();
  private final List<ParameterDeclarerContributor> fieldParameterContributors;


  public SoapModelLoaderDelegate(Class<?> extensionType, String version) {
    checkArgument(extensionType != null, format("describer %s does not specify an extension type", getClass().getName()));
    this.extensionType = extensionType;
    this.version = version;
    typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(extensionType.getClassLoader());

    fieldParameterContributors =
      ImmutableList.of(new InfrastructureFieldContributor(),
                       new ParameterTypeUnwrapperContributor(typeLoader, TypedValue.class,
                                                             new TypedValueTypeModelProperty()),
                       new ParameterTypeUnwrapperContributor(typeLoader, ParameterResolver.class,
                                                             new ParameterResolverTypeModelProperty()));
  }

  /**
   * {@inheritDoc}
   */
  public ExtensionDeclarer declare(ExtensionLoadingContext context) {
    final SoapExtensionTypeWrapper<?> soapExtensionElement = ExtensionTypeFactory.getSoapExtensionType(extensionType);
    Extension extension = MuleExtensionAnnotationParser.getExtension(extensionType);
    ExtensionDeclarer declarer =
      context.getExtensionDeclarer()
        .named(extension.name())
        .onVersion(version)
        .fromVendor(extension.vendor())
        .withCategory(extension.category())
        .withMinMuleVersion(new MuleVersion(extension.minMuleVersion()))
        .describedAs(extension.description())
        .withModelProperty(new ImplementingTypeModelProperty(extensionType));

    OperationDeclarer operation = getInvokeOperation(declarer);

    declareConfiguration(declarer, soapExtensionElement, operation);

    //soapExtensionElement.getServiceProviders().forEach(c -> declareConfiguration(declarer, soapExtensionElement, c, operation));

    return declarer;
  }

  private OperationDeclarer getInvokeOperation(ExtensionDeclarer declarer) {
    OperationDeclarer operation = declarer.withOperation("invoke")
      .withModelProperty(new OperationExecutorModelProperty(new SoapOperationExecutorFactory()));

    operation.withOutput().ofType(typeLoader.load(String.class));
    operation.withOutputAttributes().ofType(typeLoader.load(NullAttributes.class));

    operation.requiresConnection(true);

    ParameterGroupDeclarer group = operation.blocking(true)
      .withStereotype(new Stereotype("SOAP"))
      .onParameterGroup("General");

    group.withRequiredParameter("service").ofType(typeLoader.load(String.class));
    group.withRequiredParameter("operation").ofType(typeLoader.load(String.class));
    group.withRequiredParameter("request").ofType(typeLoader.load(String.class));

    return operation;
  }

  private void declareConfiguration(ExtensionDeclarer declarer,
                                    SoapExtensionTypeWrapper<?> extension,
                                    OperationDeclarer operation) {
    ConfigurationDeclarer configurationDeclarer =
      declarer.withConfig(DEFAULT_CONFIG_NAME).describedAs(DEFAULT_CONFIG_DESCRIPTION);

    TypeAwareConfigurationFactory configurationFactory =
      new TypeAwareConfigurationFactory(extension.getDeclaringClass(), extension.getDeclaringClass().getClassLoader());

    configurationDeclarer.withModelProperty(new ConfigurationFactoryModelProperty(configurationFactory))
      .withModelProperty(new ImplementingTypeModelProperty(extension.getDeclaringClass()));

    configurationDeclarer.withOperation(operation);


    extension.getServiceProviders().forEach(service -> declareServiceProvider(declarer, service));

    //extension.getConnectionProviders().forEach(prov -> declareServiceProvider(configurationDeclarer, prov));
  }

  private void declareServiceProvider(HasConnectionProviderDeclarer declarer, SoapServiceProviderWrapper serviceProvider) {
    //final Class<? extends SoapTransportProvider> providerClass = serviceProvider.getDeclaringClass();

    //ConnectionProviderDeclarer providerDeclarer = connectionProviderDeclarers.get(serviceProvider);
    //if (providerDeclarer != null) {
    //  declarer.withConnectionProvider(providerDeclarer);
    //  return;
    //}

    String name = serviceProvider.getAlias() + "connection";
    String description = serviceProvider.getDescription();

    if (serviceProvider.getName().equals(serviceProvider.getAlias())) {
      name = DEFAULT_CONNECTION_PROVIDER_NAME;
    }

    //List<Class<?>> providerGenerics = serviceProvider.getInterfaceGenerics(ConnectionProvider.class);

    //if (providerGenerics.size() != 1) {
    //   TODO: MULE-9220: Add a syntax validator for this
      //throw new IllegalConnectionProviderModelDefinitionException(
      //  format("Connection provider class '%s' was expected to have 1 generic type "
      //           + "(for the connection type) but %d were found",
      //         serviceProvider.getName(), providerGenerics.size()));
    //}

    SoapConnectionProviderFactory providerFactory = new SoapConnectionProviderFactory(SoapConnectionProvider.class,
                                                                                      extensionType.getClassLoader());

    ConnectionProviderDeclarer providerDeclarer = declarer.withConnectionProvider(name).describedAs(description)
      .withModelProperty(new ConnectionProviderFactoryModelProperty(providerFactory))
      .withModelProperty(new ConnectionTypeModelProperty(SoapClient.class))
      .withModelProperty(new ImplementingTypeModelProperty(SoapConnectionProvider.class));

    providerDeclarer.withConnectionManagementType(POOLING);

    declareParameters(providerDeclarer, serviceProvider.getParameters(), fieldParameterContributors,
                      new ParameterDeclarationContext("Connection Provider", providerDeclarer.getDeclaration()),
                      empty());
  }

  private List<ParameterDeclarer> declareParameters(ParameterizedDeclarer component,
                                                    List<? extends ExtensionParameter> parameters,
                                                    List<ParameterDeclarerContributor> contributors,
                                                    ParameterDeclarationContext declarationContext,
                                                    Optional<ParameterGroupDeclarer> parameterGroupDeclarer) {
    List<ParameterDeclarer> declarerList = new ArrayList<>();

    for (ExtensionParameter extensionParameter : parameters) {

      if (!extensionParameter.shouldBeAdvertised()) {
        continue;
      }

      if (declaredAsGroup(component, contributors, declarationContext, extensionParameter)) {
        continue;
      }

      ParameterGroupDeclarer groupDeclarer = parameterGroupDeclarer.orElseGet(component::onDefaultParameterGroup);

      ParameterDeclarer parameter;
      if (extensionParameter.isRequired()) {
        parameter = groupDeclarer.withRequiredParameter(extensionParameter.getAlias());
      } else {
        parameter = groupDeclarer.withOptionalParameter(extensionParameter.getAlias())
          .defaultingTo(extensionParameter.defaultValue().isPresent() ? extensionParameter.defaultValue().get() : null);
      }

      parameter.ofType(extensionParameter.getMetadataType(typeLoader)).describedAs(extensionParameter.getDescription());
      parseParameterRole(extensionParameter, parameter);
      parseExpressionSupport(extensionParameter, parameter);
      parseNullSafe(extensionParameter, parameter);
      parseDefaultEncoding(extensionParameter, parameter);
      addTypeRestrictions(extensionParameter, parameter);
      parseLayout(extensionParameter, parameter);
      addImplementingTypeModelProperty(extensionParameter, parameter);
      parseXmlHints(extensionParameter, parameter);
      contributors.forEach(contributor -> contributor.contribute(extensionParameter, parameter, declarationContext));
      declarerList.add(parameter);
    }

    return declarerList;
  }

  private void checkAnnotationsNotUsedMoreThanOnce(List<? extends ExtensionParameter> parameters,
                                                   Class<? extends Annotation>... annotations) {
    for (Class<? extends Annotation> annotation : annotations) {
      final long count = parameters.stream().filter(param -> param.isAnnotatedWith(annotation)).count();
      if (count > 1) {
        throw new IllegalModelDefinitionException(
          format("The defined parameters %s from %s, uses the annotation @%s more than once",
                 parameters.stream().map(p -> p.getName()).collect(toList()),
                 parameters.get(0).getOwnerDescription(), annotation.getSimpleName()));
      }
    }
  }
  private void parseDefaultEncoding(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    // TODO: MULE-9220 - Add a syntax validator which checks that the annotated parameter is a String
    if (extensionParameter.getAnnotation(DefaultEncoding.class).isPresent()) {
      parameter.getDeclaration().setRequired(false);
      parameter.withModelProperty(new DefaultEncodingModelProperty());
    }
  }

  private boolean declaredAsGroup(ParameterizedDeclarer component,
                                  List<ParameterDeclarerContributor> contributors,
                                  ParameterDeclarationContext declarationContext,
                                  ExtensionParameter groupParameter)
    throws IllegalParameterModelDefinitionException {

    ParameterGroup groupAnnotation = groupParameter.getAnnotation(ParameterGroup.class).orElse(null);
    if (groupAnnotation == null) {
      return false;
    }

    final String groupName = groupAnnotation.name();
    if (DEFAULT_GROUP_NAME.equals(groupName)) {
      throw new IllegalParameterModelDefinitionException(
        format("%s '%s' defines parameter group of name '%s' which is the default one. "
                 + "@%s cannot be used with the default group name",
               getComponentModelTypeName(component),
               ((NamedDeclaration) component.getDeclaration()).getName(),
               groupName,
               ParameterGroup.class.getSimpleName()));
    }

    final Type type = groupParameter.getType();

    final List<FieldElement> nestedGroups = type.getAnnotatedFields(ParameterGroup.class);
    if (!nestedGroups.isEmpty()) {
      throw new IllegalParameterModelDefinitionException(format(
        "Class '%s' is used as a @%s but contains fields which also hold that annotation. Nesting groups is not allowed. "
          + "Offending fields are: [%s]",
        type.getName(),
        ParameterGroup.class.getSimpleName(),
        nestedGroups.stream().map(element -> element.getName())
          .collect(joining(","))));
    }

    final List<FieldElement> annotatedParameters = type.getAnnotatedFields(Parameter.class);

    if (groupParameter.isAnnotatedWith(org.mule.runtime.extension.api.annotation.param.Optional.class)) {
      throw new IllegalParameterModelDefinitionException(format(
        "@%s can not be applied alongside with @%s. Affected parameter is [%s].",
        org.mule.runtime.extension.api.annotation.param.Optional.class
          .getSimpleName(),
        ParameterGroup.class.getSimpleName(),
        groupParameter.getName()));
    }

    ParameterGroupDeclarer declarer = component.onParameterGroup(groupName);
    if (declarer.getDeclaration().getModelProperty(ParameterGroupModelProperty.class).isPresent()) {
      throw new IllegalParameterModelDefinitionException(format("Parameter group '%s' has already been declared on %s '%s'",
                                                                groupName,
                                                                getComponentDeclarationTypeName(component.getDeclaration()),
                                                                ((NamedDeclaration) component.getDeclaration()).getName()));
    } else {
      declarer.withModelProperty(new ParameterGroupModelProperty(
        new ParameterGroupDescriptor(groupName, type, groupParameter
          .getDeclaringElement())));
    }

    type.getAnnotation(ExclusiveOptionals.class).ifPresent(annotation -> {
      Set<String> optionalParamNames = annotatedParameters.stream()
        .filter(f -> !f.isRequired())
        .map(NamedObject::getName)
        .collect(toSet());

      declarer.withExclusiveOptionals(optionalParamNames, annotation.isOneRequired());
    });


    declarer.withDslInlineRepresentation(groupAnnotation.showInDsl());

    parseLayoutAnnotations(groupParameter, LayoutModel.builder()).ifPresent(declarer::withLayout);

    if (!annotatedParameters.isEmpty()) {
      declareParameters(component, annotatedParameters, contributors, declarationContext, ofNullable(declarer));
    } else {
      declareParameters(component, getFieldsWithGetters(type.getDeclaringClass()).stream().map(FieldWrapper::new)
        .collect(toList()), contributors, declarationContext, ofNullable(declarer));
    }

    return true;
  }

  private void parseParameterRole(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    parameter.withRole(roleOf(extensionParameter.getAnnotation(Content.class)));
  }

  private void parseExpressionSupport(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    final Optional<Expression> annotation = extensionParameter.getAnnotation(Expression.class);
    if (annotation.isPresent()) {
      parameter.withExpressionSupport(getExpressionSupport(annotation.get()));
    }
  }

  private void parseNullSafe(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    if (extensionParameter.isAnnotatedWith(NullSafe.class)) {
      if (extensionParameter.isRequired() && !extensionParameter.isAnnotatedWith(ParameterGroup.class)) {
        throw new IllegalParameterModelDefinitionException(
          format("Parameter '%s' is required but annotated with '@%s', which is redundant",
                 extensionParameter.getName(), NullSafe.class.getSimpleName()));
      }

      Class<?> defaultType = extensionParameter.getAnnotation(NullSafe.class).get().defaultImplementingType();
      final boolean hasDefaultOverride = !defaultType.equals(Object.class);

      MetadataType nullSafeType = hasDefaultOverride ? typeLoader.load(defaultType) : parameter.getDeclaration().getType();

      parameter.getDeclaration().getType().accept(new BasicTypeMetadataVisitor() {

        @Override
        protected void visitBasicType(MetadataType metadataType) {
          throw new IllegalParameterModelDefinitionException(
            format("Parameter '%s' is annotated with '@%s' but is of type '%s'. That annotation can only be "
                     + "used with complex types (Pojos, Lists, Maps)",
                   extensionParameter.getName(), NullSafe.class.getSimpleName(),
                   extensionParameter.getType().getName()));
        }

        @Override
        public void visitArrayType(ArrayType arrayType) {
          if (hasDefaultOverride) {
            throw new IllegalParameterModelDefinitionException(format("Parameter '%s' is annotated with '@%s' is of type '%s'"
                                                                        + " but a 'defaultImplementingType' was provided."
                                                                        + " Type override is not allowed for Collections",
                                                                      extensionParameter.getName(),
                                                                      NullSafe.class.getSimpleName(),
                                                                      extensionParameter.getType().getName()));
          }
        }

        @Override
        public void visitObject(ObjectType objectType) {
          if (hasDefaultOverride && isMap(objectType)) {
            throw new IllegalParameterModelDefinitionException(format("Parameter '%s' is annotated with '@%s' is of type '%s'"
                                                                        + " but a 'defaultImplementingType' was provided."
                                                                        + " Type override is not allowed for Maps",
                                                                      extensionParameter.getName(),
                                                                      NullSafe.class.getSimpleName(),
                                                                      extensionParameter.getType().getName()));
          }

          if (hasDefaultOverride && isInstantiable(objectType)) {
            throw new IllegalParameterModelDefinitionException(
              format("Parameter '%s' is annotated with '@%s' is of concrete type '%s',"
                       + " but a 'defaultImplementingType' was provided."
                       + " Type override is not allowed for concrete types",
                     extensionParameter.getName(),
                     NullSafe.class.getSimpleName(),
                     extensionParameter.getType().getName()));
          }

          if (!isInstantiable(nullSafeType) && !isMap(nullSafeType)) {
            throw new IllegalParameterModelDefinitionException(
              format("Parameter '%s' is annotated with '@%s' but is of type '%s'. That annotation can only be "
                       + "used with complex instantiable types (Pojos, Lists, Maps)",
                     extensionParameter.getName(),
                     NullSafe.class.getSimpleName(),
                     extensionParameter.getType().getName()));
          }

          if (hasDefaultOverride && !getType(parameter.getDeclaration().getType()).isAssignableFrom(getType(nullSafeType))) {
            throw new IllegalParameterModelDefinitionException(
              format("Parameter '%s' is annotated with '@%s' of type '%s', but provided type '%s"
                       + " is not a subtype of the parameter's type",
                     extensionParameter.getName(),
                     NullSafe.class.getSimpleName(),
                     extensionParameter.getType().getName(),
                     getType(nullSafeType).getName()));
          }
        }
      });

      parameter.withModelProperty(new NullSafeModelProperty(nullSafeType));
    }
  }

  private void parseLayout(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    parseLayoutAnnotations(extensionParameter, LayoutModel.builder()).ifPresent(parameter::withLayout);
  }

  private void parseXmlHints(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    extensionParameter.getAnnotation(XmlHints.class).ifPresent(
      hints -> parameter.withDsl(ParameterDslConfiguration.builder()
                                   .allowsInlineDefinition(hints.allowInlineDefinition())
                                   .allowsReferences(hints.allowReferences())
                                   .allowTopLevelDefinition(hints.allowTopLevelDefinition())
                                   .build()));
  }

  private void addTypeRestrictions(WithAnnotations withAnnotations, ParameterDeclarer parameter) {
    withAnnotations.getAnnotation(RestrictedTo.class)
      .ifPresent(restrictedTo -> parameter.withModelProperty(new TypeRestrictionModelProperty<>(restrictedTo.value())));
  }

  private void addImplementingTypeModelProperty(ExtensionParameter extensionParameter, ParameterDeclarer parameter) {
    AnnotatedElement element = extensionParameter.getDeclaringElement();
    parameter.withModelProperty(element instanceof Field
                                  ? new DeclaringMemberModelProperty(((Field) element))
                                  : new ImplementingParameterModelProperty((java.lang.reflect.Parameter) element));
  }

}
