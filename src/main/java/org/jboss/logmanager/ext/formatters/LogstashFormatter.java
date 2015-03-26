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

import java.util.Collections;
import java.util.Map;

import org.jboss.logmanager.ExtLogRecord;

/**
 * A {@link org.jboss.logmanager.ext.formatters.JsonFormatter JSON formatter} which adds the {@code @version} to
 * the generated JSON and overrides the {@code timestamp} key to {@code @timestamp}.
 * <p>
 * The default {@link #getVersion() version} is {@code 1}.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LogstashFormatter extends JsonFormatter {

    private volatile int version = 1;

    /**
     * Create the lostash formatter.
     */
    public LogstashFormatter() {
        this(Collections.singletonMap(Key.TIMESTAMP, "@timestamp"));
    }

    /**
     * Create the logstash formatter overriding any default keys
     *
     * @param keyOverrides the keys used to override the defaults
     */
    public LogstashFormatter(final Map<Key, String> keyOverrides) {
        super(keyOverrides);
    }

    @Override
    protected void before(final Generator generator, final ExtLogRecord record) throws Exception {
        generator.add("@version", version);
    }

    /**
     * Returns the version being used for the {@code @version} property.
     *
     * @return the version being used
     */
    public int getVersion() {
        return version;
    }

    /**
     * Sets the version to use for the {@code @version} property.
     *
     * @param version the version to use
     */
    public void setVersion(final int version) {
        this.version = version;
    }
}
