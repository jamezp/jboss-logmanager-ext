/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager.ext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.ExtLogRecord.FormatStyle;
import org.junit.Assert;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractTest {

    protected ExtLogRecord createLogRecord(final String msg) {
        return createLogRecord(org.jboss.logmanager.Level.INFO, msg);
    }

    protected ExtLogRecord createLogRecord(final String format, final Object... args) {
        return createLogRecord(org.jboss.logmanager.Level.INFO, format, args);
    }

    protected ExtLogRecord createLogRecord(final org.jboss.logmanager.Level level, final String msg) {
        return new ExtLogRecord(level, msg, getClass().getName());
    }

    protected ExtLogRecord createLogRecord(final org.jboss.logmanager.Level level, final String format, final Object... args) {
        final ExtLogRecord record = new ExtLogRecord(level, format, FormatStyle.PRINTF, getClass().getName());
        record.setParameters(args);
        return record;
    }

    public static class MapBuilder<K, V> {
        private final Map<K, V> result;

        private MapBuilder(final Map<K, V> result) {
            this.result = result;
        }

        public static <K, V> MapBuilder<K, V> create() {
            return new MapBuilder<>(new LinkedHashMap<K, V>());
        }

        public MapBuilder<K, V> add(final K key, final V value) {
            result.put(key, value);
            return this;
        }

        public Map<K, V> build() {
            return Collections.unmodifiableMap(result);
        }
    }
}
