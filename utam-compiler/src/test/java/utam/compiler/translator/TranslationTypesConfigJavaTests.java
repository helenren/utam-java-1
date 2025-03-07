/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.compiler.translator;

import utam.core.declarative.translator.TranslationTypesConfig;
import utam.compiler.helpers.TranslationContext;
import utam.core.framework.consumer.UtamError;
import org.testng.annotations.Test;

import static utam.compiler.translator.TranslationTypesConfigJava.getWrongTypeError;
import static utam.compiler.translator.TranslatorMockUtilities.TEST_URI;
import static utam.compiler.translator.TranslatorMockUtilities.getDefaultConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.expectThrows;

/**
 * Provides tests for the TranslatorTypes class
 *
 * @author james.evans
 */
public class TranslationTypesConfigJavaTests {

  private static final TranslationTypesConfig TYPES = new TranslationTypesConfigJava();

  /** The getInterfaceType method should return a valid interface type */
  @Test
  public void testValidInterfaceTypes() {
    assertThat(
        TYPES.getInterfaceType("utam-test/pageObjects/test/testObject").getFullName(),
        is(equalTo("utam.test.pageobjects.test.TestObject")));

    assertThat(
        TYPES.getInterfaceType("utam-test/pageObjects/test/object/testObject").getFullName(),
        is(equalTo("utam.test.pageobjects.test.object.TestObject")));

    assertThat(
        TYPES.getInterfaceType("utam/pageObjects/test/testObject").getFullName(),
        is(equalTo("utam.pageobjects.test.TestObject")));
  }

  @Test
  public void testShortNamesThrows() {
    final String shortPageObjectUri = "utam-test/pageObjects";
    UtamError e = expectThrows(UtamError.class, () -> TYPES.getInterfaceType(shortPageObjectUri));
    assertThat(e.getMessage(), containsString(getWrongTypeError(shortPageObjectUri)));

    final String shortUtilityUri = "utam-test/utils";
    e = expectThrows(UtamError.class, () -> TYPES.getUtilityType(shortUtilityUri));
    assertThat(e.getMessage(), containsString(getWrongTypeError(shortUtilityUri)));
  }

  @Test
  public void testGetInterfaceTypeWithNonUtamPrefix() {
    String URI = "lwr/pageObjects/test/testObject";
    assertThat(TYPES.getInterfaceType(URI).getFullName(), is(equalTo("lwr.pageobjects.test.TestObject")));
    URI = "lwr/NotPageObjects/testObject";
    assertThat(TYPES.getInterfaceType(URI).getFullName(), is(equalTo("lwr.notpageobjects.TestObject")));
  }

  @Test
  public void testValidUtilityTypes() {
    String URI = "utam-test/utils/test/testObject";
    assertThat(TYPES.getUtilityType(URI).getFullName(), is(equalTo("utam.test.utils.test.TestObject")));
    URI = "test/shmutils/test/testObject";
    assertThat(TYPES.getUtilityType(URI).getFullName(), is(equalTo("test.shmutils.test.TestObject")));
    URI = "test/shmutils/testObject";
    assertThat(TYPES.getUtilityType(URI).getFullName(), is(equalTo("test.shmutils.TestObject")));
  }

  @Test
  public void testTypeValidationFromContext() {
    TranslationContext context = new TranslationContext(TranslatorMockUtilities.TEST_URI, getDefaultConfig());
    final String INVALID_PAGE_OBJECT_TYPE = "InvalidType";
    UtamError e = expectThrows(UtamError.class, () -> context.getType(INVALID_PAGE_OBJECT_TYPE));
    assertThat(
        e.getMessage(), containsString(getWrongTypeError(INVALID_PAGE_OBJECT_TYPE)));
  }

  @Test
  public void testIsUtamType() {
    final String INVALID_PAGE_OBJECT_TYPE = "InvalidType";
    assertThat(TranslationTypesConfigJava.isPageObjectType(INVALID_PAGE_OBJECT_TYPE), is(false));
    assertThat(TranslationTypesConfigJava.isPageObjectType(TEST_URI), is(true));
  }

  /** The getClassType method should return a valid interface type */
  @Test
  public void testGetClassType() {
    assertThat(
            TYPES.getClassType("utam-test/pageObjects/test/testObject").getFullName(),
            is(equalTo("utam.test.pageobjects.test.impl.TestObjectImpl")));
  }
}
