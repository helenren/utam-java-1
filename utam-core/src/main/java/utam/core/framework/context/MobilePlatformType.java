/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.core.framework.context;

import static utam.core.framework.context.StringValueProfile.getProfileConfigName;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;

/**
 * type of a mobile platform is used to set active profile from tests
 *
 * @author elizaveta.ivanova
 * @since 234
 */
public enum MobilePlatformType implements Profile {

  WEB,
  ANDROID,
  ANDROID_TABLET,
  ANDROID_PHONE,
  IOS,
  IOS_TABLET,
  IOS_PHONE;

  public static final String PLATFORM_PROFILE_NAME = "platform";

  public boolean isIOS() {
    return this == IOS || this == IOS_PHONE || this == IOS_TABLET;
  }

  public boolean isAndroid() {
    return this == ANDROID || this == ANDROID_PHONE || this == ANDROID_TABLET;
  }

  /**
   * detect profile based on driver type
   *
   * @param driver driver instance
   * @return profile
   */
  public static MobilePlatformType fromDriver(WebDriver driver) {
    if (driver instanceof AndroidDriver) {
      return isTablet(driver) ? ANDROID_TABLET : ANDROID_PHONE;
    }
    if (driver instanceof IOSDriver) {
      return isIPad(driver) ? IOS_TABLET : IOS_PHONE;
    }
    if (driver instanceof AppiumDriver) {
      // mock passed from test
      if(((AppiumDriver) driver).getCapabilities() == null) {
        return WEB;
      }
      Platform platform = ((AppiumDriver) driver).getCapabilities().getPlatform();
      if (platform == Platform.LINUX) {
        return isTablet(driver) ? ANDROID_TABLET : ANDROID_PHONE;
      }
      if (platform == Platform.MAC) {
        return isIPad(driver) ? IOS_TABLET : IOS_PHONE;
      }
    }
    return WEB;
  }


  @Override
  public String getName() {
    return PLATFORM_PROFILE_NAME;
  }

  @Override
  public String getValue() {
    return name().toLowerCase();
  }

  @Override
  public String getConfigName(String moduleName) {
    return getProfileConfigName(this, moduleName);
  }

  private static boolean isIPad(WebDriver driver) {
    Object deviceObject = ((AppiumDriver) driver).getSessionDetail("device");
    if (deviceObject != null) {
        return "iPad".equalsIgnoreCase(deviceObject.toString());
    }
    return false;
  }

  private static boolean isTablet(WebDriver driver) {
    Object deviceScreenSizeObject = ((AppiumDriver)driver).getSessionDetail("deviceScreenSize");
    Object deviceScreenDensityObject = ((AppiumDriver)driver).getSessionDetail("deviceScreenDensity");

    // For android, based on https://developer.android.com/training/multiscreen/screensizes
    // when device's dp is equal or bigger than 600, will be treated as tablet, otherwise will be phone
    if (deviceScreenSizeObject != null && deviceScreenDensityObject != null) {
        int dp = Integer.parseInt(deviceScreenSizeObject.toString().split("[xX]")[0])
                * 160 / Integer.parseInt(deviceScreenDensityObject.toString());
        return dp >= 600;
    }
    return false;
  }
}
