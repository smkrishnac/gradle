/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.snapshot;

import org.gradle.internal.vfs.SnapshotHierarchy;
import org.gradle.internal.vfs.impl.ChangeListenerFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;

public class SnapshotHierarchyReference {
    private final AtomicReference<SnapshotHierarchy> root;
    private final ReentrantLock updateLock = new ReentrantLock();

    public SnapshotHierarchyReference(SnapshotHierarchy root) {
        this.root = new AtomicReference<>(root);
    }

    public SnapshotHierarchy get() {
        return root.get();
    }

    public void update(UnaryOperator<SnapshotHierarchy> updateFunction, ChangeListenerFactory.LifecycleAwareChangeListener changeListener) {
        updateLock.lock();
        try {
            root.updateAndGet(current -> {
                changeListener.start();
                return updateFunction.apply(current);
            });
            changeListener.finish();
        } finally {
            updateLock.unlock();
        }
    }
}
