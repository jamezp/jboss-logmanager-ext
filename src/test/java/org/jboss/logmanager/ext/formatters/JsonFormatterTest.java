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

import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.ext.AbstractTest;
import org.jboss.logmanager.ext.formatters.StructuredFormatter.Key;
import org.junit.Before;
import org.junit.Test;

import static org.jboss.logmanager.ext.formatters.JsonTestUtil.compare;
import static org.jboss.logmanager.ext.formatters.JsonTestUtil.compareLogstash;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JsonFormatterTest extends AbstractTest {

  @Before
    public void before() {
        JsonTestUtil.KEY_OVERRIDES.clear();
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
        final Throwable t = new RuntimeException("Test cause exception");
        final Throwable dup = new IllegalStateException("Duplicate");
        t.addSuppressed(dup);
        final Throwable cause = new RuntimeException("Test Exception", t);
        dup.addSuppressed(cause);
        cause.addSuppressed(new IllegalArgumentException("Suppressed"));
        cause.addSuppressed(dup);
        record.setThrown(cause);
        record.putMdc("testMdcKey", "testMdcValue");
        record.setNdc("testNdc");
        formatter.setExceptionOutputType(JsonFormatter.ExceptionOutputType.DETAILED_AND_FORMATTED);
        compare(record, formatter);
    }

    @Test
    public void testMetaData() throws Exception {
        final JsonFormatter formatter = new JsonFormatter();
        formatter.setPrintDetails(true);
        formatter.setMetaData("context-id=context1");
        ExtLogRecord record = createLogRecord("Test formatted %s", "message");
        Map<String, String> metaDataMap = MapBuilder.<String, String>create()
                .add("context-id", "context1")
                .build();
        compare(record, formatter, metaDataMap);

        formatter.setMetaData("vendor=Red Hat\\, Inc.,product-type=JBoss");
        metaDataMap = MapBuilder.<String, String>create()
                .add("vendor", "Red Hat, Inc.")
                .add("product-type", "JBoss")
                .build();
        compare(record, formatter, metaDataMap);
    }

    @Test
    public void testLogstashFormat() throws Exception {
        JsonTestUtil.KEY_OVERRIDES.put(Key.TIMESTAMP, "@timestamp");
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

}
