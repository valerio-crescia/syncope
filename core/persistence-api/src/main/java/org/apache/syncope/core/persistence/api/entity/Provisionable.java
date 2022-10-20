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
package org.apache.syncope.core.persistence.api.entity;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.to.AbstractProvision;
import org.apache.syncope.common.lib.types.TraceLevel;

public interface Provisionable<E extends AbstractProvision> extends ProvidedKeyEntity {

    List<E> getProvisions();

    Optional<E> getProvisionByAnyType(String anyType);

    Optional<E> getProvisionByObjectClass(String objectClass);

    TraceLevel getProvisioningTraceLevel();

    void setProvisioningTraceLevel(TraceLevel provisioningTraceLevel);
}
