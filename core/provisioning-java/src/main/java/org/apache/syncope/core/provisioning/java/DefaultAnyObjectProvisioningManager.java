/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.java;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DefaultAnyObjectProvisioningManager implements AnyObjectProvisioningManager {

    @Autowired
    protected AnyObjectWorkflowAdapter awfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected VirAttrHandler virtAttrHandler;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Override
    public Pair<String, List<PropagationStatus>> create(
            final AnyObjectTO anyObjectTO, final boolean nullPriorityAsync) {

        return create(anyObjectTO, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Pair<String, List<PropagationStatus>> create(
            final AnyObjectTO anyObjectTO, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        WorkflowResult<String> created = awfAdapter.create(anyObjectTO);

        List<PropagationTaskInfo> taskInfos = propagationManager.getCreateTasks(
                AnyTypeKind.ANY_OBJECT,
                created.getResult(),
                null,
                created.getPropByRes(),
                anyObjectTO.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync);

        return Pair.of(created.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public Pair<AnyObjectUR, List<PropagationStatus>> update(
            final AnyObjectUR anyObjectUR, final boolean nullPriorityAsync) {

        return update(anyObjectUR, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Pair<AnyObjectUR, List<PropagationStatus>> update(
            final AnyObjectUR anyObjectUR, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        WorkflowResult<AnyObjectUR> updated = awfAdapter.update(anyObjectUR);

        List<PropagationTaskInfo> taskInfos = propagationManager.getUpdateTasks(
                AnyTypeKind.ANY_OBJECT,
                updated.getResult().getKey(),
                false,
                null,
                updated.getPropByRes(),
                anyObjectUR.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync);

        return Pair.of(updated.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public List<PropagationStatus> delete(final String key, final boolean nullPriorityAsync) {
        return delete(key, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public List<PropagationStatus> delete(
            final String key, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.DELETE, anyObjectDAO.findAllResourceKeys(key));

        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after awfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required user information
        List<PropagationTaskInfo> taskInfos = propagationManager.getDeleteTasks(
                AnyTypeKind.ANY_OBJECT,
                key,
                propByRes,
                excludedResources);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync);

        try {
            awfAdapter.delete(key);
        } catch (PropagationException e) {
            throw e;
        }

        return propagationReporter.getStatuses();
    }

    @Override
    public String unlink(final AnyObjectUR anyObjectUR) {
        return awfAdapter.update(anyObjectUR).getResult().getKey();
    }

    @Override
    public String link(final AnyObjectUR anyObjectUR) {
        return awfAdapter.update(anyObjectUR).getResult().getKey();
    }

    @Override
    public List<PropagationStatus> provision(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.UPDATE, resources);

        List<PropagationTaskInfo> taskInfos = propagationManager.getUpdateTasks(
                AnyTypeKind.ANY_OBJECT,
                key,
                false,
                null,
                propByRes,
                null,
                null);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync);

        return propagationReporter.getStatuses();
    }

    @Override
    public List<PropagationStatus> deprovision(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.DELETE, resources);

        List<PropagationTaskInfo> taskInfos = propagationManager.getDeleteTasks(
                AnyTypeKind.ANY_OBJECT,
                key,
                propByRes,
                anyObjectDAO.findAllResourceKeys(key).stream().
                        filter(resource -> !resources.contains(resource)).
                        collect(Collectors.toList()));
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync);

        return propagationReporter.getStatuses();
    }
}
