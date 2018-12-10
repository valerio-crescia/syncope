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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.apache.syncope.core.provisioning.api.pushpull.UserPullResultHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultUserPullResultHandler extends AbstractPullResultHandler implements UserPullResultHandler {

    @Autowired
    private UserProvisioningManager userProvisioningManager;

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.USER);
    }

    @Override
    protected String getName(final AnyTO anyTO) {
        return UserTO.class.cast(anyTO).getUsername();
    }

    @Override
    protected ProvisioningManager<?, ?> getProvisioningManager() {
        return userProvisioningManager;
    }

    @Override
    protected AnyTO getAnyTO(final String key) {
        return userDataBinder.getUserTO(key);
    }

    @Override
    protected WorkflowResult<? extends AnyUR> update(final AnyUR req) {
        WorkflowResult<Pair<UserUR, Boolean>> update = uwfAdapter.update((UserUR) req);
        return new WorkflowResult<>(update.getResult().getLeft(), update.getPropByRes(), update.getPerformedTasks());
    }

    @Override
    protected AnyTO doCreate(final AnyTO anyTO, final SyncDelta delta) {
        UserTO userTO = UserTO.class.cast(anyTO);

        Boolean enabled = pullUtils.readEnabled(delta.getObject(), profile.getTask());
        Map.Entry<String, List<PropagationStatus>> created =
                userProvisioningManager.create(userTO, true, true, enabled,
                        Collections.singleton(profile.getTask().getResource().getKey()), true);

        return getAnyTO(created.getKey());
    }

    @Override
    protected AnyUR doUpdate(
            final AnyTO before,
            final AnyUR anyUR,
            final SyncDelta delta,
            final ProvisioningReport result) {

        UserUR userUR = UserUR.class.cast(anyUR);
        Boolean enabled = pullUtils.readEnabled(delta.getObject(), profile.getTask());

        Pair<UserUR, List<PropagationStatus>> updated = userProvisioningManager.update(
                userUR,
                result,
                enabled,
                Collections.singleton(profile.getTask().getResource().getKey()),
                true);

        return updated.getLeft();
    }
}
