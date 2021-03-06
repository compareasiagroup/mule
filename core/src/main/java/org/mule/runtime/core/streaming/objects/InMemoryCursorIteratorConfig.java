/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.streaming.objects;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_OBJECT_STREAMING_BUFFER_INCREMENT_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_OBJECT_STREAMING_BUFFER_SIZE;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_OBJECT_STREAMING_MAX_BUFFER_SIZE;
import org.mule.runtime.api.streaming.objects.CursorIterator;

/**
 * Configuration for a {@link CursorIterator} which uses memory for buffering
 *
 * @since 4.0
 */
public final class InMemoryCursorIteratorConfig {

  private final int initialBufferSize;
  private final int bufferSizeIncrement;
  private final int maxInMemoryInstances;

  /**
   * @return A new instance configured with default settings
   */
  public static InMemoryCursorIteratorConfig getDefault() {
    return new InMemoryCursorIteratorConfig(DEFAULT_OBJECT_STREAMING_BUFFER_SIZE,
                                            DEFAULT_OBJECT_STREAMING_BUFFER_INCREMENT_SIZE,
                                            DEFAULT_OBJECT_STREAMING_MAX_BUFFER_SIZE);
  }

  /**
   * Creates a new instance
   *
   * @param initialBufferSize   the buffer's initial size. Must be greater than zero
   * @param bufferSizeIncrement the size that the buffer should gain each time it is expanded. A value of zero means no expansion.
   *                            Cannot be lower than zero.
   * @param maxInMemoryInstances     the maximum amount of space that the buffer can grow to. Use {@code null} for unbounded buffers
   * @throws IllegalArgumentException if any of the given arguments is invalid
   */
  public InMemoryCursorIteratorConfig(int initialBufferSize, int bufferSizeIncrement, int maxInMemoryInstances) {
    checkArgument(initialBufferSize > 0, "initialBufferSize must be greater than zero");
    checkArgument(bufferSizeIncrement >= 0, "bufferSizeIncrement cannot be negative");

    this.initialBufferSize = initialBufferSize;
    this.bufferSizeIncrement = bufferSizeIncrement;
    this.maxInMemoryInstances = maxInMemoryInstances;
  }

  public int getInitialBufferSize() {
    return initialBufferSize;
  }

  public int getBufferSizeIncrement() {
    return bufferSizeIncrement;
  }

  public int getMaxInMemoryInstances() {
    return maxInMemoryInstances;
  }
}
