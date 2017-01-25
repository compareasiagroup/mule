/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import static org.apache.ws.security.handler.WSHandlerConstants.TIMESTAMP;
import static org.apache.ws.security.handler.WSHandlerConstants.TTL_TIMESTAMP;
import static org.mule.api.config.MuleProperties.MULE_CHECK_TIMESTAMP_IN_WSS_RESPONSE;

import java.util.Map;

public class WssTimestampSecurityStrategy extends AbstractSecurityStrategy implements SecurityStrategy
{
    private long expires;
    private boolean checkResponseTimestamp;
    
    public WssTimestampSecurityStrategy()
    {
        String checkResponseTimestampValue = System.getProperty(MULE_CHECK_TIMESTAMP_IN_WSS_RESPONSE, "false");
        checkResponseTimestamp = Boolean.parseBoolean(checkResponseTimestampValue);
    }

    @Override
    public void apply(Map<String, Object> outConfigProperties, Map<String, Object> inConfigProperties)
    {
        appendAction(outConfigProperties, TIMESTAMP);
        outConfigProperties.put(TTL_TIMESTAMP, String.valueOf(expires));
        if (checkResponseTimestamp)
        {
            appendAction(inConfigProperties, TIMESTAMP);
            inConfigProperties.put(TTL_TIMESTAMP, String.valueOf(expires));
        }
    }

    public long getExpires()
    {
        return expires;
    }

    public void setExpires(long expires)
    {
        this.expires = expires;
    }

    public boolean isCheckResponseTimestamp()
    {
        return checkResponseTimestamp;
    }
}
