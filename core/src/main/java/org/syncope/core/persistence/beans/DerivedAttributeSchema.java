/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.beans;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

@Entity
public class DerivedAttributeSchema extends AbstractBaseBean {

    @Id
    private String name;
    private String expression;
    @OneToMany(mappedBy = "schema")
    private Set<DerivedAttribute> derivedAttributes;
    @ManyToMany
    private Set<AttributeSchema> attributeSchemas;

    public DerivedAttributeSchema() {
        attributeSchemas = new HashSet<AttributeSchema>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Set<DerivedAttribute> getDerivedAttributes() {
        return derivedAttributes;
    }

    public void setDerivedAttributes(Set<DerivedAttribute> derivedAttributes) {
        this.derivedAttributes = derivedAttributes;
    }

    public boolean addAttributeSchema(AttributeSchema attributeSchema) {
        return attributeSchemas.add(attributeSchema);
    }

    public boolean removeAttributeSchema(AttributeSchema attributeSchema) {
        return attributeSchemas.remove(attributeSchema);
    }

    public Set<AttributeSchema> getAttributeSchemas() {
        return attributeSchemas;
    }

    public void setAttributeSchemas(Set<AttributeSchema> attributeSchemas) {
        this.attributeSchemas = attributeSchemas;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DerivedAttributeSchema other =
                (DerivedAttributeSchema) obj;

        if ((this.name == null)
                ? (other.name != null) : !this.name.equals(other.name)) {

            return false;
        }
        if ((this.expression == null)
                ? (other.expression != null)
                : !this.expression.equals(other.expression)) {

            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 79 * hash + (this.expression != null
                ? this.expression.hashCode() : 0);

        return hash;
    }
}
