/*
 * Copyright 2017 Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>.
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
package br.ufpr.gres.testcase;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class Description {

    private final String testClass;
    private final String name;

    public Description(final String name) {
        this(name, (String) null);
    }

    public Description(final String name, final Class<?> testClass) {
        this(name, testClass.getName());
    }

    public Description(final String name, final String testClass) {
        this.testClass = internIfNotNull(testClass);
        this.name = name;
    }

    private static String internIfNotNull(final String string) {
        if (string == null) {
            return null;
        }
        return string.intern();
    }

    public String getFirstTestClass() {
        return this.testClass;
    }

    public String getQualifiedName() {
        return this.testClass != null ? this.getFirstTestClass() + '.' + this.getName() : this.getName();
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result)
                + ((this.name == null) ? 0 : this.name.hashCode());
        result = (prime * result)
                + ((this.testClass == null) ? 0 : this.testClass.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Description other = (Description) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.testClass == null) {
            if (other.testClass != null) {
                return false;
            }
        } else if (!this.testClass.equals(other.testClass)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Description [testClass=" + this.testClass + ", name=" + this.name
                + ']';
    }

}