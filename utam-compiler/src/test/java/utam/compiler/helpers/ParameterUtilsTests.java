/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.compiler.helpers;

import utam.core.declarative.representation.MethodParameter;
import utam.core.declarative.representation.TypeProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static utam.compiler.helpers.ParameterUtils.EMPTY_PARAMETERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Provides tests for the Parameter class and the nested Parameter.Regular and Parameter.Literal
 * classes
 *
 * @author james.evans
 */
public class ParameterUtilsTests {

  /** A Parameter object should be able to be created */
  @Test
  public void testParameter() {
    ParameterUtils.Regular parameter =
        new ParameterUtils.Regular("paramName", PrimitiveType.STRING);
    assertThat(parameter.isLiteral(), is(equalTo(false)));
    assertThat(parameter.getValue(), is(equalTo("paramName")));
    assertThat(parameter.getDeclaration(), is(equalTo("String paramName")));
    assertThat(parameter.getType(), is(equalTo(PrimitiveType.STRING)));
  }

  /** A Parameter.Regular object should be able to be created */
  @Test
  public void testPrimitiveParameter() {
    ParameterUtils.Regular param = new ParameterUtils.Regular("paramName", PrimitiveType.STRING);
    assertThat(param.getDeclaration(), is(equalTo("String paramName")));
    assertThat(param.isLiteral(), is(equalTo(false)));
    assertThat(param.getType().getSimpleName(), is(equalTo("String")));
    assertThat(param.getValue(), is(equalTo("paramName")));
  }

  /** A Parameter.Literal object should be able to be created */
  @Test
  public void testPrimitiveLiteral() {
    ParameterUtils.Regular param = new ParameterUtils.Literal("\"paramName\"", PrimitiveType.STRING);
    assertThat(param.getDeclaration(), is(emptyString()));
    assertThat(param.isLiteral(), is(equalTo(true)));
    assertThat(param.getType().getSimpleName(), is(equalTo("String")));
    assertThat(param.getValue(), is(equalTo("\"paramName\"")));
  }

  /**
   * Tests that the asString static method returns a string representation of a list of
   * MethodParameter objects
   */
  @Test
  public void testAsStringReturnsExpectedValue() {
    assertThat(
        ParameterUtils.getParametersValuesString(
            Arrays.asList(new MockMethodParameter("first"), new MockMethodParameter("second"))),
        is(equalTo("first, second")));
  }

  /**
   * Tests that the default implementation of the isLiteral method returns false even if a custom
   * implementation does not override the interface default method definition
   */
  @Test
  public void testDefaultIsLiteral() {
    assertThat(new MockMethodParameter("value").isLiteral(), is(equalTo(false)));
  }

  @Test
  public void testDefaultGetDeclaration() {
    assertThat(new MockMethodParameter("value").getDeclaration(), is(equalTo("FakeType value")));
  }

  /**
   * Tests that the EMPTY_PARAMETERS member of the MethodParameter interface returns an empty list
   * of MethodParameter objects
   */
  @Test
  public void testEmptyParametersMemberReturnsEmptyList() {
    assertThat(EMPTY_PARAMETERS, is(equalTo(new ArrayList<MethodParameter>())));
  }

  @Test
  public void testEquals() {
    ParameterUtils.Regular param = new ParameterUtils.Regular("paramName", PrimitiveType.STRING);
    ParameterUtils.Regular equalParam = new ParameterUtils.Regular("paramName", PrimitiveType.STRING);
    ParameterUtils.Regular otherParam = new ParameterUtils.Regular("otherParamName", PrimitiveType.NUMBER);
    assertThat(param.equals(equalParam), is(equalTo(true)));
    assertThat(param.equals(otherParam), is(equalTo(false)));
    assertThat(param.equals("notEqual"), is(equalTo(false)));
  }

  private static TypeProvider getTypeProvider() {
    String packageName = "utam.fake.package";
    String simpleName = "FakeType";
    TypeProvider provider = mock(TypeProvider.class);
    when(provider.getPackageName()).thenReturn(packageName);
    when(provider.getSimpleName()).thenReturn(simpleName);
   
    when(provider.getFullName()).thenReturn(
        String.format("%s.%s", packageName, simpleName));
    return provider;
  }
  
  private static class MockMethodParameter extends ParameterUtils.Regular {
    MockMethodParameter(String value) {
      super(value, getTypeProvider());
    }
  }
}
