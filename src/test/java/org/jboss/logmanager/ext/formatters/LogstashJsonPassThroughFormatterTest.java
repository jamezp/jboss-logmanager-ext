package org.jboss.logmanager.ext.formatters;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.ext.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

import static org.jboss.logmanager.ext.formatters.JsonTestUtil.compareLogstash;

/**
 * <p>Provides ...</p>
 * <p>
 * <p>Created on 15/03/2017 by willows_s</p>
 *
 * @author <a href="mailto:willows_s@iblocks.co.uk">willows_s</a>
 */
public class LogstashJsonPassThroughFormatterTest extends AbstractTest {

  @Test
  public void testLogstashFormat() throws Exception {
    JsonTestUtil.KEY_OVERRIDES.put(StructuredFormatter.Key.TIMESTAMP, "@timestamp");
    final LogstashJsonPassThroughFormatter formatter = new LogstashJsonPassThroughFormatter();
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

    record = createLogRecord("{");
    formatter.setVersion(2);
    compareLogstash(record, formatter, 2);
  }

  @Test
  public void testLogstashFormatAlreadyJson() throws Exception {
    final LogstashJsonPassThroughFormatter formatter = new LogstashJsonPassThroughFormatter();
    String message = "{\"testField1\":\"testField2\"}";
    ExtLogRecord record = createLogRecord(message);
    String result = formatter.format(record);
    Assert.assertEquals(message + "\n", result);

    message = "{\"testField1\":\"testField2\"}\n";
    record = createLogRecord(message);
    result = formatter.format(record);
    Assert.assertEquals(message, result);

    message = "{\"testField1\":\"testField2\"}\r\n";
    record = createLogRecord(message);
    result = formatter.format(record);
    Assert.assertEquals(message, result);
  }
}
