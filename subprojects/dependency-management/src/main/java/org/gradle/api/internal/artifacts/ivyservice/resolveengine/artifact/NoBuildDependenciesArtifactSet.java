/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact;

import org.gradle.internal.operations.BuildOperationQueue;
import org.gradle.internal.operations.RunnableBuildOperation;

public class NoBuildDependenciesArtifactSet implements ResolvedArtifactSet {
    private final ResolvedArtifactSet set;

    private NoBuildDependenciesArtifactSet(ResolvedArtifactSet set) {
        this.set = set;
    }

    public static ResolvedArtifactSet of(ResolvedArtifactSet set) {
        if (set == ResolvedArtifactSet.EMPTY || set instanceof NoBuildDependenciesArtifactSet) {
            return set;
        }
        return new NoBuildDependenciesArtifactSet(set);
    }

    @Override
    public Completion startVisit(BuildOperationQueue<RunnableBuildOperation> actions, AsyncArtifactListener listener) {
        return set.startVisit(actions, listener);
    }

    @Override
    public void collectBuildDependencies(BuildDependenciesVisitor visitor) {
    }
}
