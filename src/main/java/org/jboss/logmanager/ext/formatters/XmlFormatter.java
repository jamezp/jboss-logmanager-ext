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

import java.io.Writer;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A formatter that outputs the record in XML format.
 * <p>
 * The details include;
 * </p>
 * <ul>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceClassName() source class name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceFileName() source file name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceMethodName() source method name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceLineNumber() source line number}</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class XmlFormatter extends StructuredFormatter {

    private volatile boolean prettyPrint = false;

    /**
     * Creates a new XML formatter
     */
    public XmlFormatter() {
    }

    /**
     * Creates a new XML formatter.
     *
     * @param keyOverrides a map of the default keys to override
     */
    public XmlFormatter(final Map<Key, String> keyOverrides) {
        super(keyOverrides);
    }

    /**
     * Indicates whether or not pretty printing is enabled.
     *
     * @return {@code true} if pretty printing is enabled, otherwise {@code false}
     */
    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    /**
     * Turns on or off pretty printing.
     *
     * @param b {@code true} to turn on pretty printing or {@code false} to turn it off
     */
    public void setPrettyPrint(final boolean b) {
        prettyPrint = b;
    }

    @Override
    protected Generator createGenerator(final Writer writer) throws Exception {
        return new XmlGenerator(writer);
    }

    private class XmlGenerator extends Generator {
        private final XMLStreamWriter xmlWriter;

        private XmlGenerator(final Writer writer) throws XMLStreamException {
            final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            if (prettyPrint) {
                xmlWriter = new IndentingXmlWriter(xmlOutputFactory.createXMLStreamWriter(writer));
            } else {
                xmlWriter = xmlOutputFactory.createXMLStreamWriter(writer);
            }
        }

        @Override
        public Generator begin() throws Exception {
            writeStart(getKey(Key.RECORD));
            return this;
        }

        @Override
        public Generator add(final String key, final Map<String, ?> value) throws Exception {
            if (value == null) {
                writeEmpty(key);
            } else {
                writeStart(key);
                for (Map.Entry<String, ?> entry : value.entrySet()) {
                    final String k = entry.getKey();
                    final Object v = entry.getValue();
                    if (v == null) {
                        writeEmpty(k);
                    } else {
                        add(k, String.valueOf(v));
                    }
                }
                writeEnd();
            }
            return this;
        }

        @Override
        public Generator add(final String key, final String value) throws Exception {
            if (value == null) {
                writeEmpty(key);
            } else {
                writeStart(key);
                xmlWriter.writeCharacters(value);
                writeEnd();
            }
            return this;
        }

        @Override
        public Generator addStackTrace(final Throwable throwable) throws Exception {
            if (throwable != null) {
                writeStart(getKey(Key.EXCEPTION));
                add(getKey(Key.EXCEPTION_MESSAGE), throwable.getMessage());

                final StackTraceElement[] elements = throwable.getStackTrace();
                for (StackTraceElement e : elements) {
                    writeStart(getKey(Key.EXCEPTION_FRAME));
                    add(getKey(Key.EXCEPTION_FRAME_CLASS), e.getClassName());
                    add(getKey(Key.EXCEPTION_FRAME_METHOD), e.getMethodName());
                    final int line = e.getLineNumber();
                    if (line >= 0) {
                        add(getKey(Key.EXCEPTION_FRAME_LINE), e.getLineNumber());
                    }
                    writeEnd(); // end exception element
                }

                writeEnd(); // end exception
            }

            return this;
        }

        @Override
        public Generator end() throws Exception {
            writeEnd(); // end record
            safeFlush(xmlWriter);
            safeClose(xmlWriter);
            return this;
        }

        private void writeEmpty(final String name) throws XMLStreamException {
            xmlWriter.writeEmptyElement(name);
        }

        private void writeStart(final String name) throws XMLStreamException {
            xmlWriter.writeStartElement(name);
        }

        private void writeEnd() throws XMLStreamException {
            xmlWriter.writeEndElement();
        }

        public void safeFlush(final XMLStreamWriter flushable) {
            if (flushable != null) try {
                flushable.flush();
            } catch (Throwable ignore) {
            }
        }

        public void safeClose(final XMLStreamWriter closeable) {
            if (closeable != null) try {
                closeable.close();
            } catch (Throwable ignore) {
            }
        }
    }
}
