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

import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

/**
 * An abstract class that uses a generator to help generate structured data from a {@link
 * org.jboss.logmanager.ExtLogRecord record}.
 * <p>
 * Note that including details can be expensive in terms of calculating the caller.
 * </p>
 * <p>
 * By default the appending {@link #setAppendEndOfLine(boolean) EOL (\n)} is set to {@code true}.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class StructuredFormatter extends ExtFormatter {

    /**
     * The key used for the structured log record data.
     */
    public static enum Key {
        EXCEPTION("exception"),
        EXCEPTION_FRAME("frame"),
        EXCEPTION_FRAME_CLASS("class"),
        EXCEPTION_FRAME_LINE("line"),
        EXCEPTION_FRAME_METHOD("method"),
        EXCEPTION_FRAMES("frames"),
        EXCEPTION_MESSAGE("message"),
        LEVEL("level"),
        LOGGER_CLASS_NAME("loggerClassName"),
        LOGGER_NAME("loggerName"),
        MDC("mdc"),
        MESSAGE("message"),
        NDC("ndc"),
        RECORD("record"),
        SEQUENCE("sequence"),
        SOURCE_CLASS_NAME("sourceClassName"),
        SOURCE_FILE_NAME("sourceFileName"),
        SOURCE_LINE_NUMBER("sourceLineNumber"),
        SOURCE_METHOD_NAME("sourceMethodName"),
        THREAD_ID("threadId"),
        THREAD_NAME("threadName"),
        TIMESTAMP("timestamp");

        private final String key;

        private Key(final String key) {
            this.key = key;
        }

        /**
         * Returns the name of the key for the structure.
         *
         * @return the name of they key
         */
        public String getKey() {
            return key;
        }
    }

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private final ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            final String dateFormat = StructuredFormatter.this.datePattern;
            return new SimpleDateFormat(dateFormat == null ? DEFAULT_DATE_FORMAT : dateFormat);
        }
    };

    private final Map<Key, String> keyOverrides;
    private volatile boolean printDetails;
    private volatile String datePattern;
    private volatile boolean addEolChar = true;

    protected StructuredFormatter() {
        this(Collections.<Key, String>emptyMap());
    }

    protected StructuredFormatter(final Map<Key, String> keyOverrides) {
        this.printDetails = false;
        datePattern = DEFAULT_DATE_FORMAT;
        this.keyOverrides = keyOverrides;
    }

    /**
     * Creates the generator used to create the structured data.
     *
     * @return the generator to use
     *
     * @throws Exception if an error occurs creating the generator
     */
    protected abstract Generator createGenerator(Writer writer) throws Exception;

    /**
     * Invoked before the structured data is added to the generator.
     *
     * @param generator the generator to use
     * @param record    the log record
     */
    protected void before(final Generator generator, final ExtLogRecord record) throws Exception {
        // do nothing
    }

    /**
     * Invoked after the structured data has been added to the generator.
     *
     * @param generator the generator to use
     * @param record    the log record
     */
    protected void after(final Generator generator, final ExtLogRecord record) throws Exception {
        // do nothing
    }

    /**
     * Checks to see if the key should be overridden.
     *
     * @param defaultKey the default key
     *
     * @return the overridden key or the default key if no override exists
     */
    protected final String getKey(final Key defaultKey) {
        if (keyOverrides.containsKey(defaultKey)) {
            return keyOverrides.get(defaultKey);
        }
        return defaultKey.getKey();
    }

    @Override
    public String format(final ExtLogRecord record) {
        final boolean details = printDetails;
        try {
            // Attempt at reuse of date formatting
            final SimpleDateFormat sdf = dateFormatThreadLocal.get();
            final String pattern = this.datePattern;
            if (!sdf.toPattern().equals(pattern)) {
                sdf.applyPattern(pattern);
            }

            // Create the writer
            final StringBuilderWriter writer = new StringBuilderWriter();

            final Generator generator = createGenerator(writer).begin();
            before(generator, record);

            // Add the default structure
            generator.add(getKey(Key.TIMESTAMP), dateFormatThreadLocal.get().format(new Date(record.getMillis())))
                    .add(getKey(Key.SEQUENCE), record.getSequenceNumber())
                    .add(getKey(Key.LOGGER_CLASS_NAME), record.getLoggerClassName())
                    .add(getKey(Key.LOGGER_NAME), record.getLoggerName())
                    .add(getKey(Key.LEVEL), record.getLevel().getName())
                    .add(getKey(Key.THREAD_NAME), record.getThreadName())
                    .add(getKey(Key.MESSAGE), record.getFormattedMessage())
                    .add(getKey(Key.THREAD_ID), record.getThreadID())
                    .add(getKey(Key.MDC), record.getMdcCopy())
                    .add(getKey(Key.NDC), record.getNdc())
                    .addStackTrace(record.getThrown());
            if (details) {
                generator.add(getKey(Key.SOURCE_CLASS_NAME), record.getSourceClassName())
                        .add(getKey(Key.SOURCE_FILE_NAME), record.getSourceFileName())
                        .add(getKey(Key.SOURCE_METHOD_NAME), record.getSourceMethodName())
                        .add(getKey(Key.SOURCE_LINE_NUMBER), record.getSourceLineNumber());
            }

            after(generator, record);
            generator.end();

            // Append an EOL character if desired
            if (addEolChar) {
                writer.append('\n');
            }
            return writer.toString();
        } catch (Exception e) {
            // Wrap and rethrow
            throw new RuntimeException(e);
        }
    }

    /**
     * Indicates whether or not an EOL ({@code \n}) character will appended to the formatted message.
     *
     * @return {@code true} to append the EOL character, otherwise {@code false}
     */
    public boolean isAppendEndOfLine() {
        return addEolChar;
    }

    /**
     * Set whether or not an EOL ({@code \n}) character will appended to the formatted message.
     *
     * @param addEolChar {@code true} to append the EOL character, otherwise {@code false}
     */
    public void setAppendEndOfLine(final boolean addEolChar) {
        this.addEolChar = addEolChar;
    }

    /**
     * Gets the current date format.
     *
     * @return the current date format
     */
    public String getDateFormat() {
        return datePattern;
    }

    /**
     * Sets the pattern to use when formatting the date. The pattern must be a valid {@link java.text.SimpleDateFormat}
     * pattern.
     * <p>
     * If the pattern is {@code null} a default pattern will be used.
     * </p>
     *
     * @param pattern the pattern to use
     */
    public void setDateFormat(final String pattern) {
        if (pattern == null) {
            this.datePattern = DEFAULT_DATE_FORMAT;
        } else {
            this.datePattern = pattern;
        }
    }

    /**
     * Indicates whether or not details should be printed.
     *
     * @return {@code true} if details should be printed, otherwise {@code false}
     */
    public boolean isPrintDetails() {
        return printDetails;
    }

    /**
     * Sets whether or not details should be printed.
     * <p>
     * Printing the details can be expensive as the values are retrieved from the caller. The details include the
     * source class name, source file name, source method name and source line number.
     * </p>
     *
     * @param printDetails {@code true} if details should be printed
     */
    public void setPrintDetails(final boolean printDetails) {
        this.printDetails = printDetails;
    }

    /**
     * A generator used to create the structured output.
     */
    protected abstract static class Generator {

        /**
         * Initial method invoked at the start of the generation.
         *
         * @return the generator
         *
         * @throws Exception if an error occurs while adding the data
         */
        public Generator begin() throws Exception {
            return this;
        }

        /**
         * Writes an integer value.
         *
         * @param key   they key
         * @param value the value
         *
         * @return the generator
         *
         * @throws Exception if an error occurs while adding the data
         */
        public Generator add(final String key, final int value) throws Exception {
            add(key, Integer.toString(value));
            return this;
        }

        /**
         * Writes a long value.
         *
         * @param key   they key
         * @param value the value
         *
         * @return the generator
         *
         * @throws Exception if an error occurs while adding the data
         */
        public Generator add(final String key, final long value) throws Exception {
            add(key, Long.toString(value));
            return this;
        }

        /**
         * Writes a map value
         *
         * @param key   the key for the map
         * @param value the map
         *
         * @return the generator
         *
         * @throws Exception if an error occurs while adding the data
         */
        public abstract Generator add(String key, Map<String, ?> value) throws Exception;

        /**
         * Writes a string value.
         *
         * @param key   the key for the value
         * @param value the string value
         *
         * @return the generator
         *
         * @throws Exception if an error occurs while adding the data
         */
        public abstract Generator add(String key, String value) throws Exception;

        /**
         * Writes the stack trace.
         * <p>
         * The implementation is allowed to write the stack trace however is desirable. For example the frames of the
         * stack trace can be broken down into an array or {@link Throwable#printStackTrace()} could be used to
         * represent the stack trace.
         * </p>
         *
         * @param throwable the exception to write the stack trace for or {@code null} if there is no throwable
         *
         * @return the generator
         *
         * @throws Exception if an error occurs while adding the data
         */
        public abstract Generator addStackTrace(Throwable throwable) throws Exception;

        /**
         * Writes any trailing data that's needed.
         *
         * @return the generator
         *
         * @throws Exception if an error occurs while adding the data during the build
         */
        public abstract Generator end() throws Exception;
    }
}
