package org.jboss.logmanager.ext;

import org.junit.Assert;

import java.util.Map;

/**
 * <p>Provides ...</p>
 * <p>
 * <p>Created on 15/03/2017 by willows_s</p>
 *
 * @author <a href="mailto:willows_s@iblocks.co.uk">willows_s</a>
 */
public class TestUtil {
  public static void compareMaps(final Map<String, String> m1, final Map<String, String> m2) {
    String failureMessage = String.format("Keys did not match%n%s%n%s%n", m1.keySet(), m2.keySet());
    Assert.assertTrue(failureMessage, m1.keySet().containsAll(m2.keySet()));
    failureMessage = String.format("Values did not match%n%s%n%s%n", m1.values(), m2.values());
    Assert.assertTrue(failureMessage, m1.values().containsAll(m2.values()));
  }
}
