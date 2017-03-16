package org.jboss.logmanager.ext.formatters;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.ext.TestUtil;
import org.junit.Assert;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>Provides ...</p>
 * <p>
 * <p>Created on 15/03/2017 by willows_s</p>
 *
 * @author <a href="mailto:willows_s@iblocks.co.uk">willows_s</a>
 */
public class JsonTestUtil {
  static final Map<StructuredFormatter.Key, String> KEY_OVERRIDES = new HashMap<>();

  private static int getInt(final JsonObject json, final StructuredFormatter.Key key) {
      final String name = getKey(key);
      if (json.containsKey(name) && !json.isNull(name)) {
          return json.getInt(name);
      }
      return 0;
  }

  private static long getLong(final JsonObject json, final StructuredFormatter.Key key) {
      final String name = getKey(key);
      if (json.containsKey(name) && !json.isNull(name)) {
          return json.getJsonNumber(name).longValue();
      }
      return 0L;
  }

  private static String getString(final JsonObject json, final StructuredFormatter.Key key) {
      final String name = getKey(key);
      if (json.containsKey(name) && !json.isNull(name)) {
          return json.getString(name);
      }
      return null;
  }

  private static Map<String, String> getMap(final JsonObject json, final StructuredFormatter.Key key) {
      final String name = getKey(key);
      if (json.containsKey(name) && !json.isNull(name)) {
          final Map<String, String> result = new LinkedHashMap<>();
          final JsonObject mdcObject = json.getJsonObject(name);
          for (String k : mdcObject.keySet()) {
              final JsonValue value = mdcObject.get(k);
              if (value.getValueType() == JsonValue.ValueType.STRING) {
                  result.put(k, value.toString().replace("\"", ""));
              } else {
                  result.put(k, value.toString());
              }
          }
          return result;
      }
      return Collections.emptyMap();
  }

  private static String getKey(final StructuredFormatter.Key key) {
      if (KEY_OVERRIDES.containsKey(key)) {
          return KEY_OVERRIDES.get(key);
      }
      return key.getKey();
  }

  static void compare(final ExtLogRecord record, final ExtFormatter formatter) {
      compare(record, formatter.format(record));
  }

  static void compare(final ExtLogRecord record, final ExtFormatter formatter, final Map<String, String> metaData) {
      compare(record, formatter.format(record), metaData);
  }

  private static void compare(final ExtLogRecord record, final String jsonString) {
      compare(record, jsonString, null);
  }

  private static void compare(final ExtLogRecord record, final String jsonString, final Map<String, String> metaData) {
      final JsonReader reader = Json.createReader(new StringReader(jsonString));
      final JsonObject json = reader.readObject();
      compare(record, json, metaData);
  }

  static void compareLogstash(final ExtLogRecord record, final ExtFormatter formatter, final int version) {
      compareLogstash(record, formatter.format(record), version);
  }

  private static void compareLogstash(final ExtLogRecord record, final String jsonString, final int version) {
      final JsonReader reader = Json.createReader(new StringReader(jsonString));
      final JsonObject json = reader.readObject();
      compare(record, json, null);
      final String name = "@version";
      int foundVersion = 0;
      if (json.containsKey(name) && !json.isNull(name)) {
          foundVersion = json.getInt(name);
      }
      Assert.assertEquals(version, foundVersion);
  }

  private static void compare(final ExtLogRecord record, final JsonObject json, final Map<String, String> metaData) {
      Assert.assertEquals(record.getLevel(), Level.parse(getString(json, StructuredFormatter.Key.LEVEL)));
      Assert.assertEquals(record.getLoggerClassName(), getString(json, StructuredFormatter.Key.LOGGER_CLASS_NAME));
      Assert.assertEquals(record.getLoggerName(), getString(json, StructuredFormatter.Key.LOGGER_NAME));
      TestUtil.compareMaps(record.getMdcCopy(), getMap(json, StructuredFormatter.Key.MDC));
      Assert.assertEquals(record.getFormattedMessage(), getString(json, StructuredFormatter.Key.MESSAGE));
      Assert.assertEquals(
              new SimpleDateFormat(StructuredFormatter.DEFAULT_DATE_FORMAT).format(new Date(record.getMillis())),
              getString(json, StructuredFormatter.Key.TIMESTAMP));
      Assert.assertEquals(record.getNdc(), getString(json, StructuredFormatter.Key.NDC));
      // Assert.assertEquals(record.getResourceBundle());
      // Assert.assertEquals(record.getResourceBundleName());
      // Assert.assertEquals(record.getResourceKey());
      Assert.assertEquals(record.getSequenceNumber(), getLong(json, StructuredFormatter.Key.SEQUENCE));
      Assert.assertEquals(record.getSourceClassName(), getString(json, StructuredFormatter.Key.SOURCE_CLASS_NAME));
      Assert.assertEquals(record.getSourceFileName(), getString(json, StructuredFormatter.Key.SOURCE_FILE_NAME));
      Assert.assertEquals(record.getSourceLineNumber(), getInt(json, StructuredFormatter.Key.SOURCE_LINE_NUMBER));
      Assert.assertEquals(record.getSourceMethodName(), getString(json, StructuredFormatter.Key.SOURCE_METHOD_NAME));
      Assert.assertEquals(record.getThreadID(), getInt(json, StructuredFormatter.Key.THREAD_ID));
      Assert.assertEquals(record.getThreadName(), getString(json, StructuredFormatter.Key.THREAD_NAME));
      if (metaData != null) {
          for (String key : metaData.keySet()) {
              Assert.assertEquals(metaData.get(key), json.getString(key));
          }
      }
      // TODO (jrp) stack trace should be validated
  }
}
