package org.jboss.logmanager.ext.formatters;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ext.util.ValueParser;

/**
 * <p>A {@link JsonPassThroughFormatter} of type {@link LogstashFormatter}</p>
 * <p>Created on 15/03/2017 by Shaun Willows</p>
 *
 * @author <a href="mailto:shaun.willows@iblocks.co.uk">Shaun Willows</a>
 */
public class LogstashJsonPassThroughFormatter extends JsonPassThroughFormatter<LogstashFormatter> {

  /**
   * Constructor. Calls JsonPassThroughFormatter{@link JsonPassThroughFormatter#JsonPassThroughFormatter(ExtFormatter)} with an
   * instance of {@link LogstashFormatter}.
   */
  public LogstashJsonPassThroughFormatter() {
    super(new LogstashFormatter());
  }

  /**
   * Returns the version being used for the {@code @version} property.
   *
   * @return the version being used
   */
  public int getVersion() {
    return getDelegateFormatter().getVersion();
  }

  /**
   * Sets the version to use for the {@code @version} property.
   *
   * @param version the version to use
   */
  public void setVersion(final int version) {
    getDelegateFormatter().setVersion(version);
  }

  /**
   * Indicates whether or not pretty printing is enabled.
   *
   * @return {@code true} if pretty printing is enabled, otherwise {@code false}
   */
  public boolean isPrettyPrint() {
    return getDelegateFormatter().isPrettyPrint();
  }

  /**
   * Turns on or off pretty printing.
   *
   * @param b {@code true} to turn on pretty printing or {@code false} to turn it off
   */
  public void setPrettyPrint(final boolean b) {
    getDelegateFormatter().setPrettyPrint(b);
  }


  /**
   * Indicates whether or not an EOL ({@code \n}) character will appended to the formatted message.
   *
   * @return {@code true} to append the EOL character, otherwise {@code false}
   */
  public boolean isAppendEndOfLine() {
    return getDelegateFormatter().isAppendEndOfLine();
  }

  /**
   * Set whether or not an EOL ({@code \n}) character will appended to the formatted message.
   *
   * @param addEolChar {@code true} to append the EOL character, otherwise {@code false}
   */
  public void setAppendEndOfLine(final boolean addEolChar) {
    getDelegateFormatter().setAppendEndOfLine(addEolChar);
  }

  /**
   * Returns the value set for meta data.
   * <p>
   * The value is a string where key/value pairs are separated by commas. The key and value are separated by an
   * equal sign.
   * </p>
   *
   * @return the meta data string or {@code null} if one was not set
   * @see ValueParser#stringToMap(String)
   */
  public String getMetaData() {
    return getDelegateFormatter().getMetaData();
  }

  /**
   * Sets the meta data to use in the structured format.
   * <p>
   * The value is a string where key/value pairs are separated by commas. The key and value are separated by an
   * equal sign.
   * </p>
   *
   * @param metaData the meta data to set or {@code null} to not format any meta data
   * @see ValueParser#stringToMap(String)
   */
  public synchronized void setMetaData(final String metaData) {
    getDelegateFormatter().setMetaData(metaData);
  }

  /**
   * Gets the current date format.
   *
   * @return the current date format
   */
  public String getDateFormat() {
    return getDelegateFormatter().getDateFormat();
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
    getDelegateFormatter().setDateFormat(pattern);
  }

  /**
   * Indicates whether or not details should be printed.
   *
   * @return {@code true} if details should be printed, otherwise {@code false}
   */
  public boolean isPrintDetails() {
    return getDelegateFormatter().isPrintDetails();
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
    getDelegateFormatter().setPrintDetails(printDetails);
  }

  /**
   * Get the current output type for exceptions.
   *
   * @return the output type for exceptions
   */
  public StructuredFormatter.ExceptionOutputType getExceptionOutputType() {
    return getDelegateFormatter().getExceptionOutputType();
  }

  /**
   * Set the output type for exceptions. The default is {@link StructuredFormatter.ExceptionOutputType#DETAILED DETAILED}.
   *
   * @param exceptionOutputType the desired output type, if {@code null} {@link StructuredFormatter.ExceptionOutputType#DETAILED} is used
   */
  public void setExceptionOutputType(final StructuredFormatter.ExceptionOutputType exceptionOutputType) {
    getDelegateFormatter().setExceptionOutputType(exceptionOutputType);
  }

  /**
   * Checks the exception output type and determines if detailed output should be written.
   *
   * @return {@code true} if detailed output should be written, otherwise {@code false}
   */
  protected boolean isDetailedExceptionOutputType() {
    return getDelegateFormatter().isDetailedExceptionOutputType();
  }

  /**
   * Checks the exception output type and determines if formatted output should be written. The formatted output is
   * equivalent to {@link Throwable#printStackTrace()}.
   *
   * @return {@code true} if formatted exception output should be written, otherwide {@code false}
   */
  protected boolean isFormattedExceptionOutputType() {
    return getDelegateFormatter().isFormattedExceptionOutputType();
  }
}
