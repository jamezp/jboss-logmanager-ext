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
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.ext.AbstractTest;
import org.jboss.logmanager.ext.formatters.StructuredFormatter.Key;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class XmlFormatterTest extends AbstractTest {

    @Test
    public void testFormat() throws Exception {
        final XmlFormatter formatter = new XmlFormatter();
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

    private static int getInt(final XMLStreamReader reader) throws XMLStreamException {
        final String value = getString(reader, true);
        if (value != null) {
            return Integer.parseInt(value);
        }
        return 0;
    }

    private static long getLong(final XMLStreamReader reader) throws XMLStreamException {
        final String value = getString(reader, true);
        if (value != null) {
            return Long.parseLong(value);
        }
        return 0L;
    }

    private static String getString(final XMLStreamReader reader) throws XMLStreamException {
        return getString(reader, true);
    }

    private static String getString(final XMLStreamReader reader, boolean sanitize) throws XMLStreamException {
        final int state = reader.next();
        if (state == XMLStreamConstants.END_ELEMENT) {
            return null;
        }
        if (state == XMLStreamConstants.CHARACTERS) {
            final String text = reader.getText();
            if (sanitize) {
                return sanitize(text);
            }
            return text;
        }
        throw new IllegalStateException("No text");
    }

    private static Map<String, String> getMap(final XMLStreamReader reader) throws XMLStreamException {
        if (reader.hasNext()) {
            int state;
            final Map<String, String> result = new LinkedHashMap<>();
            while (reader.hasNext() && (state = reader.next()) != XMLStreamConstants.END_ELEMENT) {
                if (state == XMLStreamConstants.CHARACTERS) {
                    String text = sanitize(reader.getText());
                    if (text == null || text.isEmpty()) continue;
                    Assert.fail(String.format("Invalid text found: %s", text));
                }
                final String key = reader.getLocalName();
                Assert.assertTrue(reader.hasNext());
                final String value = getString(reader, true);
                Assert.assertNotNull(value);
                result.put(key, value);
            }
            return result;
        }
        return Collections.emptyMap();
    }

    private static String sanitize(final String value) {
        return value == null ? null : value.replaceAll("\n", "").trim();
    }

    private static void compare(final ExtLogRecord record, final ExtFormatter formatter) throws XMLStreamException {
        compare(record, formatter.format(record));
    }

    private static void compare(final ExtLogRecord record, final String xmlString) throws XMLStreamException {
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        final XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(xmlString));

        boolean inException = false;
        while (reader.hasNext()) {
            final int state = reader.next();
            if (state == XMLStreamConstants.END_ELEMENT && inException) {
                inException = false;
            }
            if (state == XMLStreamConstants.START_ELEMENT) {
                final String localName = reader.getLocalName();
                if (localName.equals(Key.EXCEPTION.getKey()) ||
                        localName.equals(Key.EXCEPTION_CAUSED_BY.getKey()) ||
                        localName.equals(Key.EXCEPTION_CIRCULAR_REFERENCE.getKey())) {
                    inException = true;// TODO (jrp) stack trace may need to be validated
                } else if (localName.equals(Key.LEVEL.getKey())) {
                    Assert.assertEquals(record.getLevel(), Level.parse(getString(reader)));
                } else if (localName.equals(Key.LOGGER_CLASS_NAME.getKey())) {
                    Assert.assertEquals(record.getLoggerClassName(), getString(reader));
                } else if (localName.equals(Key.LOGGER_NAME.getKey())) {
                    Assert.assertEquals(record.getLoggerName(), getString(reader));
                } else if (localName.equals(Key.MDC.getKey())) {
                    compareMap(record.getMdcCopy(), getMap(reader));
                } else if (!inException && localName.equals(Key.MESSAGE.getKey())) {
                    Assert.assertEquals(record.getFormattedMessage(), getString(reader));
                } else if (localName.equals(Key.NDC.getKey())) {
                    final String value = getString(reader);
                    Assert.assertEquals(record.getNdc(), (value == null ? "" : value));
                } else if (localName.equals(Key.SEQUENCE.getKey())) {
                    Assert.assertEquals(record.getSequenceNumber(), getLong(reader));
                } else if (localName.equals(Key.SOURCE_CLASS_NAME.getKey())) {
                    Assert.assertEquals(record.getSourceClassName(), getString(reader));
                } else if (localName.equals(Key.SOURCE_FILE_NAME.getKey())) {
                    Assert.assertEquals(record.getSourceFileName(), getString(reader));
                } else if (localName.equals(Key.SOURCE_LINE_NUMBER.getKey())) {
                    Assert.assertEquals(record.getSourceLineNumber(), getInt(reader));
                } else if (localName.equals(Key.SOURCE_METHOD_NAME.getKey())) {
                    Assert.assertEquals(record.getSourceMethodName(), getString(reader));
                } else if (localName.equals(Key.THREAD_ID.getKey())) {
                    Assert.assertEquals(record.getThreadID(), getInt(reader));
                } else if (localName.equals(Key.THREAD_NAME.getKey())) {
                    Assert.assertEquals(record.getThreadName(), getString(reader));
                } else if (localName.equals(Key.TIMESTAMP.getKey())) {
                    final SimpleDateFormat sdf = new SimpleDateFormat(StructuredFormatter.DEFAULT_DATE_FORMAT);
                    Assert.assertEquals(sdf.format(new Date(record.getMillis())), getString(reader));
                }
            }
        }
    }

    private static void compareMap(final Map<String, String> m1, final Map<String, String> m2) {
        Assert.assertEquals("Map sizes do not match", m1.size(), m2.size());
        for (String key : m1.keySet()) {
            Assert.assertTrue("Second map does not contain key " + key, m2.containsKey(key));
            Assert.assertEquals(m1.get(key), m2.get(key));
        }
    }
}
