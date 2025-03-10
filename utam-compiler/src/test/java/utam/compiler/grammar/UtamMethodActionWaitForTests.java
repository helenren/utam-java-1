/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.compiler.grammar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.testng.Assert.expectThrows;
import static utam.compiler.grammar.UtamMethodActionWaitFor.ERR_NESTED_PREDICATE_PROHIBITED;

import org.testng.annotations.Test;
import utam.compiler.helpers.TranslationContext;
import utam.compiler.representation.PageObjectValidationTestHelper;
import utam.compiler.representation.PageObjectValidationTestHelper.MethodInfo;
import utam.compiler.representation.PageObjectValidationTestHelper.MethodParameterInfo;
import utam.core.declarative.representation.PageObjectMethod;
import utam.core.framework.consumer.UtamError;

/**
 * test composed predicates
 *
 * @author elizaveta.ivanova
 * @since 236
 */
public class UtamMethodActionWaitForTests {

  private static final String methodName = "test";

  private static TranslationContext getContext(String fileName) {
    return new DeserializerUtilities().getContext("compose/predicate/" + fileName);
  }

  @Test
  public void nestedPredicateThrows() {
    String expectedError = String.format(ERR_NESTED_PREDICATE_PROHIBITED, "method 'test'");
    UtamError e = expectThrows(UtamError.class,
        () -> new DeserializerUtilities().getContext("validate/compose/nestedWait"));
    assertThat(e.getMessage(), containsString(expectedError));
  }

  @Test
  public void testWaitForCustomElement() {
    TranslationContext context = getContext("waitForCustom");
    PageObjectMethod method = context.getMethod(methodName);
    MethodInfo methodInfo = new MethodInfo(methodName, "Boolean");
    methodInfo.addParameter(new MethodParameterInfo("selectorArg", "LocatorBy"));
    methodInfo.addParameter(new MethodParameterInfo("matcherArg", "String"));
    methodInfo.addCodeLine("Boolean statement0 = this.waitFor(() -> {\n"
        + "Custom pcustom0 = this.getCustomElement();\n"
        + "if (pcustom0 == null) { return false; }\n"
        + "String pstatement0 = pcustom0.returnsString(selectorArg);\n"
        + "Boolean pmatcher0 = (pstatement0!= null && pstatement0.contains(matcherArg));\n"
        + "return pmatcher0;\n"
        + "})");
    methodInfo.addCodeLine("return statement0");
    PageObjectValidationTestHelper.validateMethod(method, methodInfo);
  }

  @Test
  public void testWaitForRootElement() {
    TranslationContext context = getContext("waitForRoot");
    MethodInfo methodInfo = new MethodInfo(methodName, "Boolean");
    methodInfo.addParameter(new MethodParameterInfo("matcherArg"));
    methodInfo.addCodeLine("Boolean statement0 = this.waitFor(() -> {\n"
        + "BasePageElement proot0 = this.getRootElement();\n"
        + "String pstatement0 = proot0.getText();\n"
        + "Boolean pmatcher0 = matcherArg.equals(pstatement0);\n"
        + "return pmatcher0;\n"
        + "})");
    methodInfo.addCodeLine("return statement0");
    PageObjectValidationTestHelper.validateMethod(context.getMethod(methodName), methodInfo);
  }

  @Test
  public void testWaitForList() {
    TranslationContext context = getContext("waitForBasicList");
    PageObjectMethod method = context.getMethod(methodName);
    MethodInfo methodInfo = new MethodInfo(methodName, "List<String>");
    methodInfo.addCodeLine("List<String> statement0 = this.waitFor(() -> {\n"
        + "List<ListElement> plist0 = this.getListElement();\n"
        + "List<String> pstatement0 = plist0.stream().map(element -> element.getText()).collect(Collectors.toList());\n"
        + "return pstatement0;\n"
        + "})");
    methodInfo.addCodeLine("return statement0");
    PageObjectValidationTestHelper.validateMethod(method, methodInfo);
  }

  @Test
  public void testComposeWaitForReturnSelf() {
    TranslationContext context = getContext("waitReturnSelf");
    PageObjectMethod method = context.getMethod(methodName);
    MethodInfo methodInfo = new MethodInfo(methodName, "Test");
    methodInfo.addCodeLine("Test statement0 = this.waitFor(() -> {\n"
        + "RootElement proot0 = this.getRoot();\n"
        + "proot0.focus();\n"
        + "return this;\n"
        + "})");
    methodInfo.addCodeLine("return statement0");
    PageObjectValidationTestHelper.validateMethod(method, methodInfo);
  }

  @Test
  public void testComposeWaitForBasicVoidAction() {
    TranslationContext context = getContext("waitVoidAction");
    PageObjectMethod method = context.getMethod(methodName);
    MethodInfo methodInfo = new MethodInfo(methodName);
    methodInfo.addCodeLine("this.waitFor(() -> {\n"
        + "RootElement proot0 = this.getRoot();\n"
        + "proot0.focus();\n"
        + "return true;\n"
        + "})");
    PageObjectValidationTestHelper.validateMethod(method, methodInfo);
  }

  @Test
  public void testWaitForWithChain() {
    TranslationContext context = getContext("waitForWithChain");
    PageObjectMethod method = context.getMethod(methodName);
    MethodInfo methodInfo = new MethodInfo(methodName, "Custom");
    methodInfo.addCodeLine("Custom statement0 = this.waitFor(() -> {\n"
        + "Custom pstatement0 = this.getCustomElement();\n"
        + "Custom pstatement1 = pstatement0.method1();\n"
        + "return pstatement1;\n"
        + "})");
    methodInfo.addCodeLine("Custom statement1 = statement0.method2()");
    methodInfo.addCodeLine("return statement1");
    String importType = "utam.test.pageobjects.Custom";
    methodInfo.addImportedTypes(importType);
    methodInfo.addImpliedImportedTypes(importType);
    PageObjectValidationTestHelper.validateMethod(method, methodInfo);
  }
}
