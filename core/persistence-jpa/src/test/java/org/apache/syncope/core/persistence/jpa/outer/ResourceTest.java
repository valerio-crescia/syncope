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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ResourceTest extends AbstractTest {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test
    public void createWithPasswordPolicy() {
        final String resourceName = "resourceWithPasswordPolicy";

        PasswordPolicy policy = policyDAO.find("986d1236-3ac5-4a19-810c-5ab21d79cba1");
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey(resourceName);
        resource.setPasswordPolicy(policy);

        ConnInstance connector = connInstanceDAO.find("88a7a819-dab5-46b4-9b90-0b9769eabdb8");
        assertNotNull(connector);
        resource.setConnector(connector);

        ExternalResource actual = resourceDAO.save(resource);
        assertNotNull(actual);

        actual = resourceDAO.find(actual.getKey());
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());

        resourceDAO.delete(resourceName);
        assertNull(resourceDAO.find(resourceName));

        assertNotNull(policyDAO.find("986d1236-3ac5-4a19-810c-5ab21d79cba1"));
    }

    @Test
    public void save() {
        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("ws-target-resource-save");

        // specify the connector
        ConnInstance connector = connInstanceDAO.find("88a7a819-dab5-46b4-9b90-0b9769eabdb8");
        assertNotNull(connector);

        resource.setConnector(connector);

        Provision provision = new Provision();
        provision.setAnyType(anyTypeDAO.findUser().getKey());
        provision.setObjectClass(ObjectClass.ACCOUNT_NAME);
        resource.getProvisions().add(provision);

        Mapping mapping = new Mapping();
        provision.setMapping(mapping);

        // specify mappings
        for (int i = 0; i < 3; i++) {
            Item item = new Item();
            item.setExtAttrName("test" + i);
            item.setIntAttrName("nonexistent" + i);
            item.setMandatoryCondition("false");
            item.setPurpose(MappingPurpose.PULL);
            mapping.add(item);
        }
        Item connObjectKey = new Item();
        connObjectKey.setExtAttrName("username");
        connObjectKey.setIntAttrName("username");
        connObjectKey.setPurpose(MappingPurpose.PROPAGATION);
        mapping.setConnObjectKeyItem(connObjectKey);

        // map a derived attribute
        Item derived = new Item();
        derived.setConnObjectKey(false);
        derived.setExtAttrName("fullname");
        derived.setIntAttrName("cn");
        derived.setPurpose(MappingPurpose.PROPAGATION);
        mapping.add(derived);

        // save the resource
        ExternalResource actual = resourceDAO.save(resource);
        entityManager().flush();
        assertNotNull(actual);
        assertNotNull(actual.getProvision(anyTypeDAO.findUser().getKey()).get().getMapping());

        entityManager().flush();
        resourceDAO.detach(actual);
        connInstanceDAO.detach(connector);

        // assign the new resource to an user
        User user = userDAO.findByUsername("rossini");
        assertNotNull(user);

        user.add(actual);

        entityManager().flush();

        // retrieve resource
        resource = resourceDAO.find(actual.getKey());
        assertNotNull(resource);
        resourceDAO.refresh(resource);

        // check connector
        connector = connInstanceDAO.find("88a7a819-dab5-46b4-9b90-0b9769eabdb8");
        assertNotNull(connector);
        assertNotNull(connector.getResources());

        assertNotNull(resource.getConnector());
        assertTrue(resource.getConnector().equals(connector));

        // check mappings
        List<Item> items = resource.getProvision(
                anyTypeDAO.findUser().getKey()).get().getMapping().getItems();
        assertNotNull(items);
        assertEquals(5, items.size());

        // check user
        user = userDAO.findByUsername("rossini");
        assertNotNull(user);
        assertNotNull(user.getResources());
        assertTrue(user.getResources().contains(actual));
    }

    @Test
    public void delete() {
        ExternalResource resource = resourceDAO.find("resource-testdb");
        assertNotNull(resource);

        // -------------------------------------
        // Get originally associated connector
        // -------------------------------------
        ConnInstance connector = resource.getConnector();
        assertNotNull(connector);
        // -------------------------------------

        // -------------------------------------
        // Get originally associated users
        // -------------------------------------
        List<User> users = userDAO.findByResource(resource);
        assertNotNull(users);

        Set<String> userKeys = users.stream().map(User::getKey).collect(Collectors.toSet());
        // -------------------------------------

        // Get tasks
        List<PropagationTask> propagationTasks = taskDAO.findAll(
                TaskType.PROPAGATION, resource, null, null, null, -1, -1, List.of());
        assertFalse(propagationTasks.isEmpty());

        // delete resource
        resourceDAO.delete(resource.getKey());

        // close the transaction
        entityManager().flush();

        // resource must be removed
        ExternalResource actual = resourceDAO.find("resource-testdb");
        assertNull(actual);

        // resource must be not referenced any more from users
        userKeys.stream().map(userDAO::find).forEach(user -> {
            assertNotNull(user);
            userDAO.findAllResources(user).
                    forEach(r -> assertFalse(r.getKey().equalsIgnoreCase(resource.getKey())));
        });

        // resource must be not referenced any more from the connector
        ConnInstance actualConnector = connInstanceDAO.find(connector.getKey());
        assertNotNull(actualConnector);
        actualConnector.getResources().
                forEach(res -> assertFalse(res.getKey().equalsIgnoreCase(resource.getKey())));

        // there must be no tasks
        propagationTasks.forEach(task -> assertTrue(taskDAO.find(task.getKey()).isEmpty()));
    }

    @Test
    public void issue243() {
        ExternalResource csv = resourceDAO.find("resource-csv");
        assertNotNull(csv);

        int origMapItems = csv.getProvision(anyTypeDAO.findUser().getKey()).get().getMapping().getItems().size();

        Item newMapItem = new Item();
        newMapItem.setIntAttrName("TEST");
        newMapItem.setExtAttrName("TEST");
        newMapItem.setPurpose(MappingPurpose.PROPAGATION);
        csv.getProvision(anyTypeDAO.findUser().getKey()).get().getMapping().add(newMapItem);

        resourceDAO.save(csv);
        entityManager().flush();

        csv = resourceDAO.find("resource-csv");
        assertNotNull(csv);
        assertEquals(
                origMapItems + 1,
                csv.getProvision(anyTypeDAO.findUser().getKey()).get().getMapping().getItems().size());
    }
}
