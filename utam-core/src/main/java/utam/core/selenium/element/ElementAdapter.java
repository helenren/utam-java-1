/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.core.selenium.element;

import static utam.core.selenium.element.DriverAdapter.ERR_SUPPORTED_FOR_MOBILE;
import static utam.core.selenium.element.DriverAdapter.find;
import static utam.core.selenium.element.DriverAdapter.findList;
import static utam.core.selenium.element.DriverAdapter.getNotFoundErr;
import static utam.core.selenium.element.DriverAdapter.getSeleniumDriver;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import utam.core.driver.Driver;
import utam.core.element.DragAndDropOptions;
import utam.core.element.Element;
import utam.core.element.FindContext;
import utam.core.element.Locator;
import utam.core.selenium.appium.MobileElementAdapter;

/**
 * implementation for selenium element
 *
 * @author elizaveta.ivanova
 * @since 234
 */
public class ElementAdapter implements Element {

  public static final String SCROLL_TOP_VIA_JAVASCRIPT =
      "return arguments[0].scrollIntoView(true);";
  public static final String SCROLL_INTO_VIEW_JS =
      "if (document.documentElement"
          + " && document.documentElement.style"
          + " && 'scrollBehavior' in document.documentElement.style) {"
          + "arguments[0].scrollIntoView({behavior: 'instant', block: 'end', inline: 'nearest'});"
          + "} else {"
          + "arguments[0].scrollIntoView(false);"
          + "}";
  static final List<Element> EMPTY_LIST = Collections.emptyList();
  static final String CLICK_VIA_JAVASCRIPT = "arguments[0].click();";
  static final String FOCUS_VIA_JAVASCRIPT = "arguments[0].focus();";
  static final String SCROLL_CENTER_VIA_JAVASCRIPT = "arguments[0].scrollIntoView({block:'center'});";
  static final String BLUR_VIA_JAVASCRIPT = "arguments[0].blur();";
  static final String ERR_NULL_ELEMENT = "Action can't be applied to an element that was not found; please check for null first";
  private static final String SCROLL_INTO_VIEW_ERR =
      "element is still not visible or clickable after scroll into view";
  private static final String SCROLL_TO_DOCUMENT_ORIGIN_JS =
      "window.scrollTo(0,0);";
  protected final Driver driverAdapter;
  private final WebElement webElement;
  private final WebDriver driver;

  public ElementAdapter(WebElement element, Driver driverAdapter) {
    this.webElement = element;
    this.driver = getSeleniumDriver(driverAdapter);
    this.driverAdapter = driverAdapter;
  }

  public WebElement getWebElement() {
    if (webElement == null) {
      throw new NullPointerException(ERR_NULL_ELEMENT);
    }
    return webElement;
  }

  private Function<WebElement, Element> getElementBuilder() {
    return element -> this instanceof MobileElementAdapter ? new MobileElementAdapter(element,
        driverAdapter) : new ElementAdapter(element, driverAdapter);
  }

  public static ElementAdapter getNullElement(Driver driverAdapter) {
    return new ElementAdapter(null, driverAdapter);
  }

  @Override
  public String toString() {
    return isNull() ? "null" : webElement.toString();
  }

  @Override
  public Element findElement(Locator by, FindContext finderContext) {
    if (webElement == null && finderContext.isNullable()) {
      return getNullElement(driverAdapter);
    }
    WebElement element = find(getScope(by, finderContext), (LocatorBy) by, finderContext);
    return element == null ? getNullElement(driverAdapter) : getElementBuilder().apply(element);
  }

  private SearchContext getScope(Locator by, FindContext findContext) {
    if (webElement == null) {
      throw new NullPointerException(getNotFoundErr(by) + ", scope element is null");
    }
    return findContext.isExpandScopeShadowRoot() ? new ShadowRootWebElement(webElement)
        : webElement;
  }

  @Override
  public List<Element> findElements(Locator by, FindContext finderContext) {
    if (webElement == null && finderContext.isNullable()) {
      return EMPTY_LIST;
    }
    List<WebElement> elements = findList(getScope(by, finderContext), (LocatorBy) by,
        finderContext);
    return elements == null ? EMPTY_LIST
        : elements.stream().map(el -> getElementBuilder().apply(el)).collect(Collectors.toList());
  }

  @Override
  public boolean isDisplayed() {
    return getWebElement().isDisplayed();
  }

  @Override
  public void clear() {
    getWebElement().clear();
  }

  @Override
  public void click() {
    getWebElement().click();
  }

  @Override
  public void deprecatedClick() {
    driverAdapter.executeScript(CLICK_VIA_JAVASCRIPT, getWebElement());
  }

  @Override
  public void scrollIntoView(ScrollOptions options) {
    if (options == ScrollOptions.TOP) {
      if (isDisplayed()) {
        return;
      }
      scrollWithCompliance(driverAdapter);
      if (isDisplayed()) {
        return;
      }
      driverAdapter.executeScript(SCROLL_TOP_VIA_JAVASCRIPT, getWebElement());
      if (!isDisplayed()) {
        driverAdapter.executeScript(SCROLL_TO_DOCUMENT_ORIGIN_JS);
        driverAdapter.executeScript(SCROLL_TOP_VIA_JAVASCRIPT, getWebElement());
      }
      if (!isDisplayed()) {
        throw new ElementNotVisibleException(SCROLL_INTO_VIEW_ERR);
      }
    } else {
      driverAdapter.executeScript(SCROLL_CENTER_VIA_JAVASCRIPT, getWebElement());
    }
  }

  private void scrollWithCompliance(Driver driver) {
    // History lesson: The original WebDriver JSON Wire Protocol, now known
    // as the OSS dialect of the protocol, had a command for getting the
    // location of an element after scrolling it into view. This was exposed
    // in Selenium by using ((Locatable)element).getCoordinates().inViewPort().
    // Drivers compliant with the W3C WebDriver Specification do not support
    // that command. In modern browsers and modern versions of Selenium,
    // all driver instances are compliant with the specification, and no
    // longer need special cases. For scrolling into view, the Selenium Java
    // language bindings require using JavaScript. Note carefully that we
    // should only attempt to scroll if either the element is not currently
    // in the view port (which should be handled by isDisplayed). The below
    // JavaScript code is designed to work across all browsers, including
    // Internet Explorer, and works around a bug in Firefox 57 and higher
    // regarding scrolling elements into view when frames are present on the
    // page.
    driver.executeScript(SCROLL_INTO_VIEW_JS, getWebElement());
  }

  @Override
  public String getAttribute(String attrName) {
    return getWebElement().getAttribute(attrName);
  }

  @Override
  public String getText() {
    return getWebElement().getText();
  }

  @Override
  public void setText(String text) {
    getWebElement().sendKeys(text);
  }

  @Override
  public int containsElements(Locator by, boolean isExpandShadowRoot) {
    List<Element> found = this.findElements(by, FindContext.Type.build(true, isExpandShadowRoot));
    if (found == null) {
      return 0;
    }
    return found.size();
  }

  @Override
  public boolean isEnabled() {
    return getWebElement().isEnabled();
  }

  @Override
  public boolean isExisting() {
    if (isNull()) {
      return false;
    }
    // try apply any action to the element
    try {
      getWebElement().isDisplayed();
      return true;
    } catch (StaleElementReferenceException | NoSuchElementException e) {
      return false;
    }
  }

  @Override
  public boolean isNull() {
    return this.webElement == null;
  }

  @Override
  public void moveTo() {
    Actions actions = new Actions(driver);
    actions.moveToElement(getWebElement()).perform();
  }

  @Override
  public boolean hasFocus() {
    return driver
        .switchTo()
        .activeElement()
        .equals(getWebElement());
  }

  @Override
  public void blur() {
    driverAdapter.executeScript(BLUR_VIA_JAVASCRIPT, getWebElement());
  }

  @Override
  public void focus() {
    driverAdapter.executeScript(FOCUS_VIA_JAVASCRIPT, getWebElement());
  }

  @Override
  public void flick(int xOffset, int yOffset) {
    throw new IllegalStateException(ERR_SUPPORTED_FOR_MOBILE);
  }

  private boolean isHoldDurationZero(DragAndDropOptions options) {
    return options.getHoldDuration() == null || options.getHoldDuration().isZero();
  }

  private WebElement getTargetElement(DragAndDropOptions options) {
    if (options.getTargetElement() == null) {
      return null;
    }
    return ((ElementAdapter) options.getTargetElement()).getWebElement();
  }

  @Override
  public void dragAndDrop(DragAndDropOptions options) {

    WebElement source = getWebElement();
    WebElement target = getTargetElement(options);

    // create an object of Actions class to build composite actions
    Actions builder = new Actions(driver);

    if (isHoldDurationZero(options)) {
      // if duration is zero - use standard Selenium action
      if (target != null) {
        builder.dragAndDrop(source, target);
      } else {
        builder.dragAndDropBy(source, options.getXOffset(), options.getYOffset());
      }
    } else {
      // if duration is set - start from moving to source, click and hold
      builder
          .moveToElement(source)
          .clickAndHold(source)
          .pause(options.getHoldDuration());
      if (target != null) {
        builder
            .moveToElement(target)
            .pause(options.getHoldDuration())
            .release(target);
      } else {
        builder
            .moveByOffset(options.getXOffset(), options.getYOffset())
            .pause(options.getHoldDuration())
            .release();
      }
    }

    // perform the drag and drop action
    builder.build().perform();
  }
}
