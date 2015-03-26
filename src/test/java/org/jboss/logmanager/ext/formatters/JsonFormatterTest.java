/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.logmanager.ext.formatters;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.ext.AbstractTest;
import org.jboss.logmanager.ext.formatters.StructuredFormatter.Key;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JsonFormatterTest extends AbstractTest {
    private static final Map<Key, String> KEY_OVERRIDES = new HashMap<>();

    @Before
    public void before() {
        KEY_OVERRIDES.clear();
    }

    @Test
    public void testFormat() throws Exception {
        final JsonFormatter formatter = new JsonFormatter();
        formatter.setPrintDetails(true);
        ExtLogRecord record = createLogRecord("Test formatted %s", "message");
        compare(record, formatter);

        record = createLogRecord("Test Message");
        compare(record, formatter);

        record = createLogRecord(Level.ERROR, "Test formatted %s", "message");
        record.setLoggerName("org.jboss.logmanager.ext.test");
        record.setMillis(System.currentTimeMillis());
        record.setThrown(new RuntimeException("Test Exception"));
        record.putMdc("testMdcKey", "testMdcValue");
        record.setNdc("testNdc");
        compare(record, formatter);
    }

    @Test
    public void testLogstashFormat() throws Exception {
        KEY_OVERRIDES.put(Key.TIMESTAMP, "@timestamp");
        final LogstashFormatter formatter = new LogstashFormatter();
        formatter.setPrintDetails(true);
        ExtLogRecord record = createLogRecord("Test formatted %s", "message");
        compareLogstash(record, formatter, 1);

        record = createLogRecord("Test Message");
        formatter.setVersion(2);
        compareLogstash(record, formatter, 2);

        record = createLogRecord(Level.ERROR, "Test formatted %s", "message");
        record.setLoggerName("org.jboss.logmanager.ext.test");
        record.setMillis(System.currentTimeMillis());
        record.setThrown(new RuntimeException("Test Exception"));
        record.putMdc("testMdcKey", "testMdcValue");
        record.setNdc("testNdc");
        compareLogstash(record, formatter, 2);
    }

    private static int getInt(final JsonObject json, final Key key) {
        final String name = getKey(key);
        if (json.containsKey(name) && !json.isNull(name)) {
            return json.getInt(name);
        }
        return 0;
    }

    private static long getLong(final JsonObject json, final Key key) {
        final String name = getKey(key);
        if (json.containsKey(name) && !json.isNull(name)) {
            return json.getJsonNumber(name).longValue();
        }
        return 0L;
    }

    private static String getString(final JsonObject json, final Key key) {
        final String name = getKey(key);
        if (json.containsKey(name) && !json.isNull(name)) {
            return json.getString(name);
        }
        return null;
    }

    private static Map<String, String> getMap(final JsonObject json, final Key key) {
        final String name = getKey(key);
        if (json.containsKey(name) && !json.isNull(name)) {
            final Map<String, String> result = new LinkedHashMap<>();
            final JsonObject mdcObject = json.getJsonObject(name);
            for (String k : mdcObject.keySet()) {
                final JsonValue value = mdcObject.get(k);
                if (value.getValueType() == ValueType.STRING) {
                    result.put(k, value.toString().replace("\"", ""));
                } else {
                    result.put(k, value.toString());
                }
            }
            return result;
        }
        return Collections.emptyMap();
    }

    private static String getKey(final Key key) {
        if (KEY_OVERRIDES.containsKey(key)) {
            return KEY_OVERRIDES.get(key);
        }
        return key.getKey();
    }

    private static void compare(final ExtLogRecord record, final ExtFormatter formatter) {
        compare(record, formatter.format(record));
    }

    private static void compare(final ExtLogRecord record, final String jsonString) {
        final JsonReader reader = Json.createReader(new StringReader(jsonString));
        final JsonObject json = reader.readObject();
        compare(record, json);
    }

    private static void compareLogstash(final ExtLogRecord record, final ExtFormatter formatter, final int version) {
        compareLogstash(record, formatter.format(record), version);
    }

    private static void compareLogstash(final ExtLogRecord record, final String jsonString, final int version) {
        final JsonReader reader = Json.createReader(new StringReader(jsonString));
        final JsonObject json = reader.readObject();
        compare(record, json);
        final String name = "@version";
        int foundVersion = 0;
        if (json.containsKey(name) && !json.isNull(name)) {
            foundVersion = json.getInt(name);
        }
        Assert.assertEquals(version, foundVersion);
    }

    private static void compare(final ExtLogRecord record, final JsonObject json) {
        Assert.assertEquals(record.getLevel(), Level.parse(getString(json, Key.LEVEL)));
        Assert.assertEquals(record.getLoggerClassName(), getString(json, Key.LOGGER_CLASS_NAME));
        Assert.assertEquals(record.getLoggerName(), getString(json, Key.LOGGER_NAME));
        compareMap(record.getMdcCopy(), getMap(json, Key.MDC));
        Assert.assertEquals(record.getFormattedMessage(), getString(json, Key.MESSAGE));
        Assert.assertEquals(
                new SimpleDateFormat(StructuredFormatter.DEFAULT_DATE_FORMAT).format(new Date(record.getMillis())),
                getString(json, Key.TIMESTAMP));
        Assert.assertEquals(record.getNdc(), getString(json, Key.NDC));
        // Assert.assertEquals(record.getResourceBundle());
        // Assert.assertEquals(record.getResourceBundleName());
        // Assert.assertEquals(record.getResourceKey());
        Assert.assertEquals(record.getSequenceNumber(), getLong(json, Key.SEQUENCE));
        Assert.assertEquals(record.getSourceClassName(), getString(json, Key.SOURCE_CLASS_NAME));
        Assert.assertEquals(record.getSourceFileName(), getString(json, Key.SOURCE_FILE_NAME));
        Assert.assertEquals(record.getSourceLineNumber(), getInt(json, Key.SOURCE_LINE_NUMBER));
        Assert.assertEquals(record.getSourceMethodName(), getString(json, Key.SOURCE_METHOD_NAME));
        Assert.assertEquals(record.getThreadID(), getInt(json, Key.THREAD_ID));
        Assert.assertEquals(record.getThreadName(), getString(json, Key.THREAD_NAME));
        // TODO (jrp) stack trace should be validated
    }

    private static void compareMap(final Map<String, String> m1, final Map<String, String> m2) {
        Assert.assertEquals("Map sizes do not match", m1.size(), m2.size());
        for (String key : m1.keySet()) {
            Assert.assertTrue("Second map does not contain key " + key, m2.containsKey(key));
            Assert.assertEquals(m1.get(key), m2.get(key));
        }
    }
}
