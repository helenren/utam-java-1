/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.core.driver;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import utam.core.element.Element;
import utam.core.element.FindContext;
import utam.core.element.Locator;

/**
 * Driver interface allows to plugin any driver type, default is Selenium
 *
 * @author elizaveta.ivanova
 * @since 234
 */
public interface Driver {

  /**
   * executes given javascript
   *
   * @param script     string with javascript code
   * @param parameters parameters passed to the script
   * @return result of the script execution
   */
  Object executeScript(String script, Object... parameters);

  /**
   * find element inside driver
   *
   * @param by            selector used to find an element
   * @param finderContext indicates conditions of the search, ex. element being nullable
   * @return if finder context is nullable can return null
   */
  Element findElement(Locator by, FindContext finderContext);

  /**
   * find multiple elements inside driver
   *
   * @param by            selector used to find elements
   * @param finderContext indicates conditions of the search, ex. element being nullable
   * @return if finder context is nullable can return null
   */
  List<Element> findElements(Locator by, FindContext finderContext);

  /**
   * polling wait repeatedly applies expectations until truthy value is return (not null or boolean
   * true)
   *
   * @param timeout timeout after which exception is thrown if condition is not met. If passed as null, timeout
   *                from config is used
   * @param isTrue  condition to apply
   * @param message error message to throw if timeout is reached, can be null
   * @param <T>     return type
   * @return result of the applied expectations
   */
  <T> T waitFor(Supplier<T> isTrue, String message, Duration timeout);

  /**
   * enters a frame or iframe element
   *
   * @param element the frame element to enter
   */
  void enterFrame(Element element);

  /**
   * exits focus from a frame or iframe to the immediate parent frame, or a no-op
   * if already on the top-level frame
   */
  void exitToParentFrame();

  /**
   * exits focus from a frame or iframe to the immediate parent frame, or a no-op
   * if already on the top-level frame
   */
  void exitFrame();

  /**
   * set active page context to NATIVE_APP
   */
  void setPageContextToNative();

  /**
   * set active page context to the target WebView page, if view is different from current, new
   * driver is created
   *
   * @param title         title to switch to
   */
  void setPageContextToWebView(String title);

  /**
   * check if current context is native
   *
   * @return boolean true if current context is native
   */
  boolean isNative();

  /**
   * check if current context is mobile
   *
   * @return boolean true if current context is native
   */
  boolean isMobile();

  /**
   * get current URL
   *
   * @return string with URL
   */
  String getUrl();

  /**
   * get current context, wraps AppiumDriver.getContext
   *
   * @return string with current context
   */
  String getContext();

  /**
   * get Driver configuration parameters
   *
   * @return driver configuration parameters
   */
  DriverConfig getDriverConfig();
}
