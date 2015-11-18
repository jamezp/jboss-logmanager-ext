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

import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParsingException;

import org.jboss.logmanager.ExtLogRecord;

/**
 * A formatter that outputs the record into JSON format optionally printing details.
 * <p/>
 * Note that including details can be expensive in terms of calculating the caller.
 * <p/>
 * The details include;
 * <ul>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceClassName() source class name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceFileName() source file name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceMethodName() source method name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceLineNumber() source line number}</li>
 * </ul>
 * <p/>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JsonFormatter extends StructuredFormatter {

    private final Map<String, Object> config;
    private Map<String,String> additionalProperties;

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
                generator.writeStartObject(getKey(Key.EXCEPTION));
                add(getKey(Key.EXCEPTION_MESSAGE), throwable.getMessage());

                generator.writeStartArray(getKey(Key.EXCEPTION_FRAMES));
                final StackTraceElement[] elements = throwable.getStackTrace();
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
                generator.writeEnd(); // end exception
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

    @Override
    protected void after(Generator generator, ExtLogRecord record) throws Exception {
        super.after(generator, record);
        if (additionalProperties != null) {
            for (Map.Entry<String, String> entry : additionalProperties.entrySet()) {
                generator.add(entry.getKey(), entry.getValue());
            }
        }
    }
    /**
     * Sets the additionalValues as json map string to be added to the log message.
     *
     * @throws JsonParsingException if the json string is invalid.
     * @throws IllegalArgumentException if the json string does not only contain string and number values.
     * @param additionalPropertiesJson the additional properties to use
     */
    public void setAdditionalValuesJson(String additionalPropertiesJson) throws JsonParsingException, IllegalArgumentException {
        Map<String, String> props = null;
        if (additionalPropertiesJson != null && !additionalPropertiesJson.trim().isEmpty()) {
            try (JsonReader reader = Json.createReader(new StringReader(additionalPropertiesJson))) {
                JsonObject jsonObject = reader.readObject();
                props = new HashMap<>();
                for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                    switch (entry.getValue().getValueType()) {
                        case STRING:
                            props.put(entry.getKey(), ((JsonString) entry.getValue()).getString());
                            break;
                        case NUMBER:
                            props.put(entry.getKey(), entry.getValue().toString());
                            break;
                        default:
                            throw new IllegalArgumentException("Only strig and number values are allowed in additional properties '"
                                + additionalPropertiesJson + "'.");
                    }
                }
            }
        }
        setAdditionalProperties(props);
    }

    /**
     * Sets the additionalValues to be added to the log message.
     *
     * @param additionalProperties the additional properties to use
     */
    public void setAdditionalProperties(Map<String, String> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
    /**
     * Returns the additional properties being used for the {@code @version} property.
     *
     * @return the additional properties being added
     */
    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }
}
