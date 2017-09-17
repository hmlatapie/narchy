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
package br.ufpr.gres.core.classpath.old;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;

/**
 *
 * @author Jackson Antonio do Prado Lima <jacksonpradolima at gmail.com>
 * @version 1.0
 */
public class OtherClassLoaderClassPathRoot implements ClassPathRoot {

    private final ClassLoader loader;

    public OtherClassLoaderClassPathRoot(final ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public Collection<String> classNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getData(final String name) {
        // TODO will this work for archives? Need to consider remote hetrogenous os
        return this.loader.getResourceAsStream(name.replace(".", "/") + ".class");
    }

    @Override
    public URL getResource(final String name) {
        return this.loader.getResource(name);
    }

    @Override
    public Optional<String> cacheLocation() {
        return Optional.empty();
    }
}
