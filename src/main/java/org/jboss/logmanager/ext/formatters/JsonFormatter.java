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

import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonGeneratorFactory;

/**
 * A formatter that outputs the record into JSON format optionally printing details.
 * <p>
 * Note that including details can be expensive in terms of calculating the caller.
 * </p>
 * <p>The details include;</p>
 * <ul>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceClassName() source class name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceFileName() source file name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceMethodName() source method name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceLineNumber() source line number}</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings("unused")
public class JsonFormatter extends StructuredFormatter {

    private final Map<String, Object> config;

    /**
     * Creates a new JSON formatter.
     */
    public JsonFormatter() {
        this(Collections.<Key, String>emptyMap());
    }

    /**
     * Creates a new JSON formatter.
     *
     * @param keyOverrides a map of overrides for the default keys
     */
    public JsonFormatter(final Map<Key, String> keyOverrides) {
        super(keyOverrides);
        config = new HashMap<>();
    }

    /**
     * Indicates whether or not pretty printing is enabled.
     *
     * @return {@code true} if pretty printing is enabled, otherwise {@code false}
     */
    public boolean isPrettyPrint() {
        synchronized (config) {
            return (config.containsKey(javax.json.stream.JsonGenerator.PRETTY_PRINTING) ? (Boolean) config.get(javax.json.stream.JsonGenerator.PRETTY_PRINTING) : false);
        }
    }

    /**
     * Turns on or off pretty printing.
     *
     * @param b {@code true} to turn on pretty printing or {@code false} to turn it off
     */
    public void setPrettyPrint(final boolean b) {
        synchronized (config) {
            if (b) {
                config.put(javax.json.stream.JsonGenerator.PRETTY_PRINTING, true);
            } else {
                config.remove(javax.json.stream.JsonGenerator.PRETTY_PRINTING);
            }
        }
    }

    @Override
    protected Generator createGenerator(final Writer writer) {
        return new JsonGenerator(writer);
    }

    private class JsonGenerator extends Generator {
        private final javax.json.stream.JsonGenerator generator;
        private int stackTraceId = 0;

        private JsonGenerator(final Writer writer) {
            final Map<String, Object> config;
            synchronized (JsonFormatter.this.config) {
                config = new HashMap<>(JsonFormatter.this.config);
            }
            final JsonGeneratorFactory factory = Json.createGeneratorFactory(config);
            generator = factory.createGenerator(writer);
        }

        @Override
        public Generator begin() {
            generator.writeStartObject();
            return this;
        }

        @Override
        public Generator add(final String key, final int value) {
            generator.write(key, value);
            return this;
        }

        @Override
        public Generator add(final String key, final long value) {
            generator.write(key, value);
            return this;
        }

        @Override
        public Generator add(final String key, final Map<String, ?> value) {
            generator.writeStartObject(key);
            if (value != null) {
                for (Map.Entry<String, ?> entry : value.entrySet()) {
                    writeObject(entry.getKey(), entry.getValue());
                }
            }
            generator.writeEnd();
            return this;
        }

        @Override
        public Generator add(final String key, final String value) {
            if (value == null) {
                generator.writeNull(key);
            } else {
                generator.write(key, value);
            }
            return this;
        }

        @Override
        public Generator addStackTrace(final Throwable throwable) throws Exception {
            if (throwable != null) {
                if (isDetailedExceptionOutputType()) {
                    // Use the identity of the throwable to determine uniqueness
                    final Map<Throwable, Integer> seen = new IdentityHashMap<>();
                    generator.writeStartArray(getKey(Key.EXCEPTION));
                    addStackTraceEntry(throwable, seen);
                    generator.writeEnd();
                }

                if (isFormattedExceptionOutputType()) {
                    final StringBuilderWriter writer = new StringBuilderWriter();
                    throwable.printStackTrace(new PrintWriter(writer));
                    generator.write(getKey(Key.STACK_TRACE), writer.toString());
                }
            }
            return this;
        }

        @Override
        public Generator end() {
            generator.writeEnd(); // end record
            generator.flush();
            generator.close();
            return this;
        }

        private void addStackTraceEntry(final Throwable throwable, final Map<Throwable, Integer> seen) {
            if (throwable == null) {
                return;
            }
            if (seen.containsKey(throwable)) {
                generator.writeStartObject();
                generator.writeStartObject(getKey(Key.EXCEPTION_CIRCULAR_REFERENCE));
                generator.write(getKey(Key.EXCEPTION_MESSAGE), throwable.getMessage());
                generator.write(getKey(Key.EXCEPTION_REFERENCE_ID), seen.get(throwable));
                generator.writeEnd(); // end circular ref object
                generator.writeEnd(); // end main object
            } else {
                seen.put(throwable, stackTraceId++);
                generator.writeStartObject();
                add(getKey(Key.EXCEPTION_MESSAGE), throwable.getMessage());
                add(getKey(Key.EXCEPTION_REFERENCE_ID), stackTraceId);
                addStackTraceElements(throwable.getStackTrace());

                // Add suppressed messages
                final Throwable[] suppressed = throwable.getSuppressed();
                if (suppressed != null && suppressed.length > 0) {
                    generator.writeStartArray(getKey(Key.EXCEPTION_SUPPRESSED));
                    for (Throwable s : suppressed) {
                        addStackTraceEntry(s, seen);
                    }
                    generator.writeEnd(); // end suppressed
                }

                // Add the cause
                final Throwable cause = throwable.getCause();
                if (cause != null) {
                    generator.writeStartArray(getKey(Key.EXCEPTION_CAUSED_BY));
                    addStackTraceEntry(cause, seen);
                    generator.writeEnd(); // end cause array
                }
                generator.writeEnd(); // end exception object
            }
        }

        private void addStackTraceElements(final StackTraceElement[] elements) {
            generator.writeStartArray(getKey(Key.EXCEPTION_FRAMES));
            for (StackTraceElement e : elements) {
                generator.writeStartObject();
                add(getKey(Key.EXCEPTION_FRAME_CLASS), e.getClassName());
                add(getKey(Key.EXCEPTION_FRAME_METHOD), e.getMethodName());
                final int line = e.getLineNumber();
                if (line >= 0) {
                    add(getKey(Key.EXCEPTION_FRAME_LINE), e.getLineNumber());
                }
                generator.writeEnd(); // end exception element
            }
            generator.writeEnd(); // end array
        }

        private void writeObject(final String key, final Object obj) {
            if (obj == null) {
                if (key == null) {
                    generator.writeNull();
                } else {
                    generator.writeNull(key);
                }
            } else if (obj instanceof Boolean) {
                final Boolean value = (Boolean) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof Integer) {
                final Integer value = (Integer) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof Long) {
                final Long value = (Long) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof Double) {
                final Double value = (Double) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof BigInteger) {
                final BigInteger value = (BigInteger) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof BigDecimal) {
                final BigDecimal value = (BigDecimal) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof String) {
                final String value = (String) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof JsonValue) {
                final JsonValue value = (JsonValue) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else {
                final String value = String.valueOf(obj);
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            }
        }
    }
}
