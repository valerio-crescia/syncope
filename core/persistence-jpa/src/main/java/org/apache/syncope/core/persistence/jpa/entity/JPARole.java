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
package org.apache.syncope.core.persistence.jpa.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import org.apache.syncope.core.persistence.api.entity.Application;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPADynRoleMembership;
import org.apache.syncope.core.persistence.jpa.validation.entity.RoleCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPARole.TABLE)
@Cacheable
@RoleCheck
public class JPARole extends AbstractProvidedKeyEntity implements Role {

    private static final long serialVersionUID = -7657701119422588832L;

    public static final String TABLE = "SyncopeRole";

    @Lob
    private String entitlements;

    @Transient
    private Set<String> entitlementsSet = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "realm_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "role_id", "realm_id" }))
    @Valid
    private List<JPARealm> realms = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "dynamicRealm_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "role_id", "dynamicRealm_id" }))
    @Valid
    private List<JPADynRealm> dynRealms = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "role")
    @Valid
    private JPADynRoleMembership dynMembership;

    @Lob
    private String anyLayout;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "role_id"),
            inverseJoinColumns =
            @JoinColumn(name = "privilege_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "role_id", "privilege_id" }))
    @Valid
    private Set<JPAPrivilege> privileges = new HashSet<>();

    @Override
    public Set<String> getEntitlements() {
        return entitlementsSet;
    }

    @Override
    public boolean add(final Realm realm) {
        checkType(realm, JPARealm.class);
        return realms.contains((JPARealm) realm) || realms.add((JPARealm) realm);
    }

    @Override
    public List<? extends Realm> getRealms() {
        return realms;
    }

    @Override
    public boolean add(final DynRealm dynamicRealm) {
        checkType(dynamicRealm, JPADynRealm.class);
        return dynRealms.contains((JPADynRealm) dynamicRealm) || dynRealms.add((JPADynRealm) dynamicRealm);
    }

    @Override
    public List<? extends DynRealm> getDynRealms() {
        return dynRealms;
    }

    @Override
    public DynRoleMembership getDynMembership() {
        return dynMembership;
    }

    @Override
    public void setDynMembership(final DynRoleMembership dynMembership) {
        checkType(dynMembership, JPADynRoleMembership.class);
        this.dynMembership = (JPADynRoleMembership) dynMembership;
    }

    @Override
    public String getAnyLayout() {
        return anyLayout;
    }

    @Override
    public void setAnyLayout(final String anyLayout) {
        this.anyLayout = anyLayout;
    }

    @Override
    public boolean add(final Privilege privilege) {
        checkType(privilege, JPAPrivilege.class);
        return privileges.add((JPAPrivilege) privilege);
    }

    @Override
    public Set<? extends Privilege> getPrivileges(final Application application) {
        return privileges.stream().
                filter(privilege -> privilege.getApplication().equals(application)).
                collect(Collectors.toSet());
    }

    @Override
    public Set<? extends Privilege> getPrivileges() {
        return privileges;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getEntitlements().clear();
        }
        if (entitlements != null) {
            getEntitlements().addAll(
                    POJOHelper.deserialize(entitlements, new TypeReference<Set<String>>() {
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
        entitlements = POJOHelper.serialize(getEntitlements());
    }
}
