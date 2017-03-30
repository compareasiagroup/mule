/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import static java.lang.Math.floor;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.util.FunctionalUtils.or;
import org.mule.runtime.api.streaming.exception.StreamingBufferSizeExceededException;
import org.mule.runtime.core.internal.streaming.object.iterator.StreamingIterator;
import org.mule.runtime.core.streaming.objects.InMemoryCursorIteratorConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * An {@link AbstractObjectStreamBuffer} implementation which uses buckets for locating items.
 * <p>
 * Because this buffer needs to provide random access, array based lists are optimal for obtaining
 * the item in a particular position. However, expanding the capacity of an array based list is very expensive.
 * <p>
 * This buffer works by partitioning the items into buckets of array based lists, so that we never need to expand
 * a list, we simply add a new bucket.
 *
 * @param <T> The generic type of the items in the stream
 * @sice 4.0
 */
public class BucketedObjectStreamBuffer<T> extends AbstractObjectStreamBuffer<T> {

  private final StreamingIterator<T> stream;
  private final InMemoryCursorIteratorConfig config;

  private List<MutableBucket<T>> buckets;
  private MutableBucket<T> currentBucket;
  private Position currentPosition;
  private Position maxPosition = null;
  private int instancesCount = 0;

  public BucketedObjectStreamBuffer(StreamingIterator<T> stream, InMemoryCursorIteratorConfig config) {
    this.stream = stream;
    this.config = config;
    initialiseBuckets();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Optional<Bucket<T>> doGet(Position position) {
    if (maxPosition != null && maxPosition.compareTo(position) < 0) {
      throw new NoSuchElementException();
    }

    return withReadLock(() -> {
      Optional<Bucket<T>> bucket = getPresentBucket(position);
      if (bucket.isPresent()) {
        return forwarding(bucket);
      }

      releaseReadLock();
      return fetch(position);
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean doHasNext(long i) {
    Position position = toPosition(i);

    return withReadLock(() -> {
      if (maxPosition != null) {
        return position.compareTo(maxPosition) < 1;
      }

      if (position.compareTo(currentPosition) < 1) {
        return true;
      }

      releaseReadLock();
      try {
        return fetch(position).isPresent();
      } catch (NoSuchElementException e) {
        return false;
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doClose() {
    closeSafely(stream::close);
    buckets.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return stream.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Position toPosition(long position) {
    int initialBufferSize = config.getInitialBufferSize();
    int bucketsDelta = config.getBufferSizeIncrement();

    if (position < initialBufferSize || bucketsDelta == 0) {
      return new Position(0, (int) position);
    }

    long offset = position - initialBufferSize;

    int bucketIndex = (int) floor(offset / bucketsDelta) + 1;
    int itemIndex = (int) position - (initialBufferSize + ((bucketIndex - 1) * bucketsDelta));

    return new Position(bucketIndex, itemIndex);
  }

  private void initialiseBuckets() {
    int size = stream.size();
    if (size > 0) {
      maxPosition = toPosition(size - 1);
      buckets = new ArrayList<>(maxPosition.getBucketIndex() + 1);
    } else {
      buckets = new ArrayList<>();
    }

    currentBucket = new MutableBucket<>(0, config.getInitialBufferSize());
    buckets.add(currentBucket);
    currentPosition = new Position(0, -1);
  }

  private Optional<Bucket<T>> getPresentBucket(Position position) {
    if (position.getBucketIndex() < buckets.size()) {
      return ofNullable(buckets.get(position.getBucketIndex()));
    }

    return empty();
  }

  private Optional<Bucket<T>> fetch(Position position) {
    return withWriteLock(() -> {
      Optional<Bucket<T>> presentBucket = getPresentBucket(position);
      if (presentBucket.filter(bucket -> bucket.contains(position)).isPresent()) {
        return presentBucket;
      }

      while (currentPosition.compareTo(position) < 0) {
        if (!stream.hasNext()) {
          maxPosition = currentPosition;
          return empty();
        }

        T item = stream.next();
        if (currentBucket.add(item)) {
          currentPosition = currentPosition.advanceItem();
        } else {
          currentBucket = mutableBucket(item, currentBucket.index + 1, config.getBufferSizeIncrement());
          buckets.add(currentBucket);
          currentPosition = currentPosition.advanceBucket();
        }
        instancesCount++;
        validateMaxBufferSizeNotExceeded();
      }

      return of(currentBucket);
    });
  }

  private Optional<Bucket<T>> forwarding(Optional<Bucket<T>> presentBucket) {
    return presentBucket.map(b -> new ForwardingBucket<>((MutableBucket<T>) b));
  }

  private void validateMaxBufferSizeNotExceeded() {
    if (instancesCount > config.getMaxInMemoryInstances()) {
      throw new StreamingBufferSizeExceededException(config.getMaxInMemoryInstances());
    }
  }

  private <T> MutableBucket<T> mutableBucket(T initialItem, int index, int capacity) {
    MutableBucket<T> bucket = new MutableBucket<>(index, capacity);
    bucket.add(initialItem);

    return bucket;
  }

  private class MutableBucket<T> implements Bucket<T> {

    private final List<T> items;
    private final int capacity;
    private final int index;

    private MutableBucket(int index, int capacity) {
      this.index = index;
      this.capacity = capacity;
      this.items = new ArrayList<>(capacity);
    }

    @Override
    public Optional<T> get(int index) {
      return withReadLock(() -> {
        if (index < items.size()) {
          return ofNullable(items.get(index));
        }
        return empty();
      });
    }

    @Override
    public boolean contains(Position position) {
      return withReadLock(() -> index == position.getBucketIndex() && position.getItemIndex() < items.size());
    }

    private boolean add(T item) {
      return withWriteLock(() -> {
        if (items.size() < capacity) {
          items.add(item);
          return true;
        }

        return false;
      });
    }
  }

  private class ForwardingBucket<T> implements Bucket<T> {

    private MutableBucket<T> delegate;

    public ForwardingBucket(MutableBucket<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public Optional<T> get(int index) {
      return withReadLock(() -> {
        Optional<T> item = delegate.get(index);
        if (item.isPresent()) {
          return item;
        }

        Position position = new Position(delegate.index, index);
        releaseReadLock();
        return withWriteLock(() -> {
          Optional<T> updatedItem = delegate.get(index);
          if (updatedItem.isPresent()) {
            return updatedItem;
          }
          delegate = (MutableBucket<T>) fetch(position).orElseThrow(NoSuchElementException::new);
          return or(delegate.get(index), () -> {
            throw new NoSuchElementException();
          });
        });
      });
    }

    @Override
    public boolean contains(Position position) {
      return delegate.contains(position);
    }
  }
}
