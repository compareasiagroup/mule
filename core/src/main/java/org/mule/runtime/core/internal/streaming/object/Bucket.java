package org.mule.runtime.core.internal.streaming.object;

import java.util.Optional;

public interface Bucket<T> {

  Optional<T> get(int index);

  boolean contains(Position position);
}
