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

package org.gradle.internal.vfs.watch.impl;

import com.google.common.base.Stopwatch;
import org.gradle.internal.snapshot.FileSystemNode;
import org.gradle.internal.vfs.watch.FileWatcherRegistry;
import org.gradle.internal.vfs.watch.WatchingNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class ThreadedFileWatcherRegistry implements FileWatcherRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedFileWatcherRegistry.class);

    private final FileWatcherRegistry delegate;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private volatile boolean closed;

    public ThreadedFileWatcherRegistry(FileWatcherRegistry delegate) {
        this.delegate = delegate;
    }

    @Override
    public void updateMustWatchDirectories(Collection<File> updatedWatchDirectories) {
        try {
            Stopwatch stopWatch = Stopwatch.createStarted();
            submitAction(watcher -> watcher.updateMustWatchDirectories(updatedWatchDirectories)).get(2, TimeUnit.SECONDS);
            LOGGER.info("Updating watched directories took {}ms", stopWatch.elapsed(TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            throw new WatchingNotSupportedException("Error while updating must watch directories", e);
        }
    }

    private Future<?> submitAction(Consumer<FileWatcherRegistry> action) {
        if (closed) {
            return CompletableFuture.completedFuture(null);
        }
        return executorService.submit(() -> {
            if (!closed) {
                action.accept(delegate);
            }
        });
    }

    @Override
    public FileWatchingStatistics getAndResetStatistics() {
        return delegate.getAndResetStatistics();
    }

    @Override
    public void close() throws IOException {
        try {
            Future<?> closer = closeAsynchronously();
            closer.get(2, TimeUnit.SECONDS);
            executorService.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to stop executer service", e);
        }
    }

    public Future<?> closeAsynchronously() {
        if (!closed) {
            closed = true;
            Future<Object> closingFuture = executorService.submit(() -> {
                delegate.close();
                return null;
            });
            executorService.shutdown();
            return closingFuture;
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void changed(Collection<FileSystemNode> removedNodes, Collection<FileSystemNode> addedNodes) {
        try {
            changedAsynchronously(removedNodes, addedNodes).get(2, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to update watches", e);
        }
    }

    public Future<?> changedAsynchronously(Collection<FileSystemNode> removedNodes, Collection<FileSystemNode> addedNodes) {
        return submitAction(watcher -> watcher.changed(removedNodes, addedNodes));
    }
}
