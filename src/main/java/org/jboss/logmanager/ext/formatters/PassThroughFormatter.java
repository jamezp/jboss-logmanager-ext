package org.jboss.logmanager.ext.formatters;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

/**
 * <p>A formatter where {@link #format(ExtLogRecord)} returns the raw message from
 * {@link ExtLogRecord#getMessage()} if the conditions applied by
 * {@link #passthrough(ExtLogRecord)} are met, other it delegates to
 * {@link #getDelegateFormatter() delegateFormatter}</p>
 * <p>This formatter is useful when the message has already been formatted appropriately
 * prior to reaching the JBoss Log Manager formatter.</p>
 * <p>Created on 15/03/2017 by Shaun Willows</p>
 *
 * @author <a href="mailto:shaun.willows@iblocks.co.uk">Shaun Willows</a>
 */
public abstract class PassThroughFormatter<T extends ExtFormatter> extends ExtFormatter {

  protected static final char LINE_FEED = '\n';

  private T delegateFormatter;


  /**
   * Constructor
   *
   * @param delegateFormatter The delegate formatter to which {@link #format(ExtLogRecord)} defers if the conditions applied by
   *                          {@link #passthrough(ExtLogRecord)} are not met.
   */
  public PassThroughFormatter(T delegateFormatter) {
    this.delegateFormatter = delegateFormatter;
  }

  /**
   * @return The delegate formatter to which {@link #format(ExtLogRecord)} defers if the conditions applied by
   * {@link #passthrough(ExtLogRecord)} are not met.
   */
  protected T getDelegateFormatter() {
    return delegateFormatter;
  }

  /**
   * Applies a set of conditions to determine whether the the raw message from
   * {@link ExtLogRecord#getMessage()} should be returned by {@link #format(ExtLogRecord)},
   * whether it should defer to {@link #getDelegateFormatter() delegateFormatter}.
   *
   * @param extLogRecord The {@link ExtLogRecord} to be formatted
   * @return true if the raw message from {@link ExtLogRecord#getMessage()} should be returned
   * by {@link #format(ExtLogRecord)}
   */
  protected abstract boolean passthrough(ExtLogRecord extLogRecord);

  /**
   * Returns the raw message from {@link ExtLogRecord#getMessage()} if the conditions applied by
   * {@link #passthrough(ExtLogRecord)} are met, other it delegates to {@link #getDelegateFormatter() delegateFormatter}
   * to generate the formatted message.
   *
   * @param extLogRecord The {@link ExtLogRecord} to be formatted
   * @return Either {@link ExtLogRecord#getMessage()} or the result of {@link ExtFormatter#format(ExtLogRecord)} for
   * {@link #getDelegateFormatter() delegateFormatter}
   */
  @Override
  public String format(ExtLogRecord extLogRecord) {
    if (passthrough(extLogRecord)) {
      String message = extLogRecord.getMessage();
      if (message.charAt(message.length() - 1) != LINE_FEED) {
        message += LINE_FEED;
      }
      return message;

    }
    return delegateFormatter.format(extLogRecord);
  }
}
