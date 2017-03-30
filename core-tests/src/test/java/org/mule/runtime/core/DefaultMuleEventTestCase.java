/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.tck.size.SmallTest;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DefaultMuleEventTestCase extends AbstractMuleContextTestCase {

  public static final Charset CUSTOM_ENCODING = UTF_8;
  public static final String PROPERTY_NAME = "test";
  public static final String PROPERTY_VALUE = "foo";

  private Message muleMessage = Message.builder().payload("test-data").build();
  private Flow flow;
  private EventContext messageContext;
  private Event muleEvent;

  @Before
  public void before() throws Exception {
    flow = getTestFlow(muleContext);
    messageContext = DefaultEventContext.create(flow, TEST_CONNECTOR);
    muleEvent = Event.builder(messageContext).message(muleMessage).flow(flow).build();
  }

  @Test
  public void setFlowVariableDefaultDataType() throws Exception {
    muleEvent = Event.builder(muleEvent).addVariable(PROPERTY_NAME, PROPERTY_VALUE).build();

    DataType dataType = muleEvent.getVariable(PROPERTY_NAME).getDataType();
    assertThat(dataType, DataTypeMatcher.like(String.class, MediaType.ANY, null));
  }

  @Test
  public void setFlowVariableCustomDataType() throws Exception {
    DataType dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

    muleEvent = Event.builder(muleEvent).addVariable(PROPERTY_NAME, PROPERTY_VALUE, dataType).build();

    DataType actualDataType = muleEvent.getVariable(PROPERTY_NAME).getDataType();
    assertThat(actualDataType, DataTypeMatcher.like(String.class, APPLICATION_XML, CUSTOM_ENCODING));
  }

  @Test
  public void setSessionVariableDefaultDataType() throws Exception {
    muleEvent.getSession().setProperty(PROPERTY_NAME, PROPERTY_VALUE);

    DataType dataType = muleEvent.getSession().getPropertyDataType(PROPERTY_NAME);
    assertThat(dataType, DataTypeMatcher.like(String.class, MediaType.ANY, null));
  }

  @Test
  public void setSessionVariableCustomDataType() throws Exception {
    DataType dataType = DataType.builder().type(String.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();

    muleEvent.getSession().setProperty(PROPERTY_NAME, PROPERTY_VALUE, dataType);

    DataType actualDataType = muleEvent.getSession().getPropertyDataType(PROPERTY_NAME);
    assertThat(actualDataType, DataTypeMatcher.like(String.class, APPLICATION_XML, CUSTOM_ENCODING));
  }
}
