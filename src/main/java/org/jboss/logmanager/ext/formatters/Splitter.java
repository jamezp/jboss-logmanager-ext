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

package org.jboss.logmanager.ext.formatters;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class Splitter {

    private static class SplitIterator implements Iterator<String> {

        private final String value;
        private final char delimiter;
        private int index;

        private SplitIterator(final String value, final char delimiter) {
            this.value = value;
            this.delimiter = delimiter;
            index = 0;
        }

        @Override
        public boolean hasNext() {
            return index != -1;
        }

        @Override
        public String next() {
            final int index = this.index;
            if (index == -1) {
                throw new NoSuchElementException();
            }
            int x = value.indexOf(delimiter, index);
            try {
                return x == -1 ? value.substring(index) : value.substring(index, x);
            } finally {
                this.index = (x == -1 ? -1 : x + 1);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static Iterator<String> iterator(final String value, final char delimiter) {
        return new SplitIterator(value, delimiter);
    }
}
