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
package org.apache.syncope.core.persistence.jpa.entity.am;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AuthModuleState;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAAuthModule.TABLE)
public class JPAAuthModule extends AbstractProvidedKeyEntity implements AuthModule {

    public static final String TABLE = "AuthModule";

    private static final long serialVersionUID = 5681033638234853077L;

    private String description;

    @Enumerated(EnumType.STRING)
    @NotNull
    private AuthModuleState authModuleState;

    @NotNull
    private Integer authModuleOrder = 0;

    @Lob
    private String items;

    @Transient
    private final List<Item> itemList = new ArrayList<>();

    @Lob
    private String jsonConf;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public AuthModuleState getState() {
        return authModuleState;
    }

    @Override
    public void setState(final AuthModuleState state) {
        this.authModuleState = state;
    }

    @Override
    public int getOrder() {
        return Optional.ofNullable(authModuleOrder).orElse(0);
    }

    @Override
    public void setOrder(final int order) {
        this.authModuleOrder = order;
    }

    @Override
    public List<Item> getItems() {
        return itemList;
    }

    @Override
    public AuthModuleConf getConf() {
        AuthModuleConf conf = null;
        if (!StringUtils.isBlank(jsonConf)) {
            conf = POJOHelper.deserialize(jsonConf, AuthModuleConf.class);
        }

        return conf;
    }

    @Override
    public void setConf(final AuthModuleConf conf) {
        jsonConf = POJOHelper.serialize(conf);
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getItems().clear();
        }
        if (items != null) {
            getItems().addAll(
                    POJOHelper.deserialize(items, new TypeReference<List<Item>>() {
                    }));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2list(true);
    }

    @PrePersist
    @PreUpdate
    public void list2json() {
        items = POJOHelper.serialize(getItems());
    }

}
