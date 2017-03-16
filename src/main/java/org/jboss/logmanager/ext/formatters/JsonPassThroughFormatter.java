package org.jboss.logmanager.ext.formatters;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

/**
 * <p>A {@link PassThroughFormatter} where the {@link PassThroughFormatter#passthrough(ExtLogRecord)}
 * implementation includes conditions to determine whether {@link ExtLogRecord#getMessage()} returns
 * a JSON message String.</p>
 * <p>A JSON message String is identified as follows:
 * <ul>
 *   <li>Must start with "{"</li>
 *   <li>Must end with "}" OR "}\n" OR "}\n\n"</li>
 * </ul></p>
 * <p>Created on 15/03/2017 by Shaun Willows</p>
 *
 * @author <a href="mailto:shaun.willows@iblocks.co.uk">Shaun Willows</a>
 */
public class JsonPassThroughFormatter<T extends ExtFormatter> extends PassThroughFormatter<T> {

  private static final char JSON_START_CHAR = '{';

  private static final char JSON_END_CHAR = '}';

  private static final char CARRIAGE_RETURN = '\r';

  /**
   * Constructor
   *
   * @param delegateFormatter The delegate formatter to which {@link #format(ExtLogRecord)} defers if the conditions applied by
   *                          {@link #passthrough(ExtLogRecord)} are not met.
   */
  public JsonPassThroughFormatter(T delegateFormatter) {
    super(delegateFormatter);
  }

  /**
   * Includes conditions to determine whether {@link ExtLogRecord#getMessage()} returns
   * a JSON message String.
   * <p>A JSON message String is identified as follows:
   * <ul>
   *   <li>Must start with "{"</li>
   *   <li>Must end with "}" OR "}\n" OR "}\n\n"</li>
   * </ul></p>
   * @param extLogRecord The {@link ExtLogRecord} to be formatted
   * @return true if {@link ExtLogRecord#getMessage()} returns a JSON message String.
   */
  @Override
  protected boolean passthrough(ExtLogRecord extLogRecord) {
    String message = extLogRecord.getMessage();
    int length = message.length();
    return length >= 2 && message.charAt(0) == JSON_START_CHAR && (message.charAt(length - 1) == JSON_END_CHAR || (
            message.charAt(length - 1) == LINE_FEED && (message.charAt(length - 2) == JSON_END_CHAR || (
                    message.charAt(length - 2) == CARRIAGE_RETURN && message.charAt(length - 3) == JSON_END_CHAR))));
  }
}
