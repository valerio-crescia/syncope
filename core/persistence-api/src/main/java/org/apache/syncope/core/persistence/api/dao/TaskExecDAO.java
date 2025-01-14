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
package org.apache.syncope.core.persistence.api.dao;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;

public interface TaskExecDAO extends DAO<TaskExec<?>> {

    <T extends Task<T>> TaskExec<T> find(TaskType type, String key);

    Optional<TaskExec<?>> find(String key);

    List<TaskExec<?>> findRecent(int max);

    TaskExec<?> findLatestStarted(TaskType type, Task<?> task);

    TaskExec<?> findLatestEnded(TaskType type, Task<?> task);

    int count(Task<?> task);

    List<TaskExec<?>> findAll(Task<?> task, int page, int itemsPerPage, List<OrderByClause> orderByClauses);

    List<TaskExec<?>> findAll(
            Task<?> task,
            OffsetDateTime startedBefore,
            OffsetDateTime startedAfter,
            OffsetDateTime endedBefore,
            OffsetDateTime endedAfter);

    <T extends Task<T>> TaskExec<T> save(TaskExec<T> execution);

    <T extends Task<T>> void saveAndAdd(TaskType type, String taskKey, TaskExec<T> execution);

    <T extends Task<T>> void delete(TaskType type, String key);

    <T extends Task<T>> void delete(TaskExec<T> execution);
}
