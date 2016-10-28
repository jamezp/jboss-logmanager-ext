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
import java.util.IdentityHashMap;
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
@SuppressWarnings("unused")
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
        private int stackTraceId = 0;

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
                if (isDetailedExceptionOutputType()) {
                    // Use the identity of the throwable to determine uniqueness
                    final Map<Throwable, Integer> seen = new IdentityHashMap<>();
                    addStackTraceDetail(throwable, seen);
                }

                if (isFormattedExceptionOutputType()) {
                    final StringBuilderWriter writer = new StringBuilderWriter();
                    throwable.printStackTrace(new PrintWriter(writer));
                    add(getKey(Key.STACK_TRACE), writer.toString());
                }
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

        private void writeExceptionMessage(final Throwable throwable, final int id) throws XMLStreamException {
            writeStart(getKey(Key.EXCEPTION_MESSAGE));
            xmlWriter.writeAttribute(getKey(Key.EXCEPTION_REFERENCE_ID), Integer.toString(id));
            if (throwable.getMessage() != null) {
                xmlWriter.writeCharacters(throwable.getMessage());
            }
            writeEnd(); // end exception message
        }

        private void addStackTraceDetail(final Throwable throwable, final Map<Throwable, Integer> seen) throws Exception {
            if (throwable == null) {
                return;
            }
            if (seen.containsKey(throwable)) {
                writeStart(getKey(Key.EXCEPTION_CIRCULAR_REFERENCE));
                writeExceptionMessage(throwable, seen.get(throwable));
                writeEnd(); // end circular reference
            } else {
                final int id = stackTraceId++;
                seen.put(throwable, id);
                writeStart(getKey(Key.EXCEPTION));
                writeExceptionMessage(throwable, id);

                final StackTraceElement[] elements = throwable.getStackTrace();
                for (StackTraceElement e : elements) {
                    writeStart(getKey(Key.EXCEPTION_FRAME));
                    add(getKey(Key.EXCEPTION_FRAME_CLASS), e.getClassName());
                    add(getKey(Key.EXCEPTION_FRAME_METHOD), e.getMethodName());
                    final int line = e.getLineNumber();
                    if (line >= 0) {
                        add(getKey(Key.EXCEPTION_FRAME_LINE), e.getLineNumber());
                    }
                    writeEnd(); // end exception frame
                }

                writeEnd(); // end exception

                // Render the suppressed messages
                final Throwable[] suppressed = throwable.getSuppressed();
                if (suppressed != null && suppressed.length > 0) {
                    writeStart(getKey(Key.EXCEPTION_SUPPRESSED));
                    for (Throwable s : suppressed) {
                        addStackTraceDetail(s, seen);
                    }
                    writeEnd();
                }

                // Render the cause
                final Throwable cause = throwable.getCause();
                if (cause != null) {
                    writeStart(getKey(Key.EXCEPTION_CAUSED_BY));
                    addStackTraceDetail(cause, seen);
                    writeEnd();
                }
            }
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
