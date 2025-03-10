/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.core.selenium.appium;

import io.appium.java_client.AppiumDriver;
import java.util.Set;
import java.util.function.Supplier;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import utam.core.driver.Driver;
import utam.core.driver.DriverConfig;
import utam.core.framework.consumer.UtamError;
import utam.core.framework.context.MobilePlatformType;
import utam.core.selenium.element.DriverAdapter;

/**
 * Appium Driver wrapper
 *
 * @author qren
 * @since 232
 */
@SuppressWarnings("rawtypes")
public class MobileDriverAdapter extends DriverAdapter implements Driver {

  static final String WEBVIEW_CONTEXT_HANDLE_PREFIX = "WEBVIEW";
  static final String NATIVE_CONTEXT_HANDLE = "NATIVE_APP";
  static final String ERR_BRIDGE_TITLE_NULL =
      "Bridge application title is null, please configure";

  private final MobilePlatformType mobilePlatform;

  public MobileDriverAdapter(AppiumDriver driver, DriverConfig driverConfig) {
    super(driver, driverConfig);
    this.mobilePlatform = MobilePlatformType.fromDriver(driver);
  }

  static AppiumDriver getAppiumDriver(Driver driver) {
    return ((MobileDriverAdapter) driver).getAppiumDriver();
  }

  final boolean isIOSPlatform() {
    return mobilePlatform == MobilePlatformType.IOS ||
            mobilePlatform == MobilePlatformType.IOS_PHONE ||
            mobilePlatform == MobilePlatformType.IOS_TABLET;
  }

  Boolean isWebViewAvailable() {
    AppiumDriver appiumDriver = getAppiumDriver();
    Set<String> contextHandles = appiumDriver.getContextHandles();
    return contextHandles.stream().
        anyMatch(handle -> handle.contains(WEBVIEW_CONTEXT_HANDLE_PREFIX));
  }

  AppiumDriver switchToWebView(String title) {
    AppiumDriver appiumDriver = getAppiumDriver();
    if (!isIOSPlatform()) {
        // Set current context to native to get the updated available contexts
        // Otherwise, the closed webview that is the current context will not be dropped
        // from the return of getContextHandles. This is Android unique. 
        setPageContextToNative();
    }
    Set<String> contextHandles = appiumDriver.getContextHandles();
    for (String contextHandle : contextHandles) {
      if (!contextHandle.equals(NATIVE_CONTEXT_HANDLE)) {
        AppiumDriver newDriver = (AppiumDriver) appiumDriver.context(contextHandle);
        String newTitle = newDriver.getTitle();
        if (!newTitle.isEmpty() && newTitle.equalsIgnoreCase(title)) {
          return newDriver;
        }
      }
    }
    // For the Appium chromedriver limitation to handle multiple WebViews,
    // If switch to context fail to find the target WebView, then switch to
    // use window
    if (mobilePlatform == MobilePlatformType.ANDROID ||
        mobilePlatform == MobilePlatformType.ANDROID_PHONE ||
        mobilePlatform == MobilePlatformType.ANDROID_TABLET) {
      Set<String> windowHandles = appiumDriver.getWindowHandles();
      for (String windowHandle : windowHandles) {
        AppiumDriver newDriver = (AppiumDriver) appiumDriver.switchTo().window(windowHandle);
        String currentTitle = newDriver.getTitle();
        if (!currentTitle.isEmpty() && currentTitle.equalsIgnoreCase(title)) {
          return newDriver;
        }
      }
    }
    return null;
  }

  @Override
  public void setPageContextToNative() {
    MobileDriverUtils.setContextToNative(getAppiumDriver());
  }

  @Override
  public void setPageContextToWebView(String title) {
    if (title == null) {
      throw new UtamError(ERR_BRIDGE_TITLE_NULL);
    }
    waitFor(this::isWebViewAvailable, "wait for web view", null);
    AppiumDriver newDriver = waitFor(() -> switchToWebView(title), "switch to web view", null);
    resetDriver(newDriver);
  }

  @Override
  public boolean isNative() {
    return MobileDriverUtils.isNative(getAppiumDriver());
  }

  AppiumDriver getAppiumDriver() {
    return (AppiumDriver) getSeleniumDriver();
  }

  @Override
  public boolean isMobile() {
    return true;
  }

  @Override
  public String getContext() {
    return getAppiumDriver().getContext();
  }

  final WebElement getWebViewElement() {
    if (isIOSPlatform()) {
      return getAppiumDriver().findElement(By.className("XCUIElementTypeWebView"));
    }
    return getAppiumDriver().findElement(By.className("android.webkit.WebView"));
  }

  // for tests
  final <T> T waitFor(Supplier<T> isTrue) {
    return waitFor(isTrue, null, null);
  }
}
