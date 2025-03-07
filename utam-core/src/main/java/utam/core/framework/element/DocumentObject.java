/*
 * Copyright (c) 2021, salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: MIT
 * For full license text, see the LICENSE file in the repo root
 * or https://opensource.org/licenses/MIT
 */
package utam.core.framework.element;

import static utam.core.element.FindContext.Type.NULLABLE;
import static utam.core.framework.base.FrameElementImpl.getUnwrappedElement;
import static utam.core.framework.base.PageObjectsFactoryImpl.getRootLocator;

import java.util.function.Supplier;
import utam.core.driver.Document;
import utam.core.driver.Driver;
import utam.core.element.Locator;
import utam.core.framework.UtamCoreError;
import utam.core.framework.base.PageObjectsFactory;
import utam.core.framework.base.RootPageObject;
import utam.core.element.FrameElement;

/**
 * implementation of the document object
 *
 * @author elizaveta.ivanova
 * @since 234
 */
public class DocumentObject implements Document {

  static final String DOM_READY_JAVASCRIPT = "document.readyState === 'complete'";
  static final String ERR_CANT_ENTER_NULL_FRAME = "Can't enter null frame element";

  private final Driver driver;
  private final PageObjectsFactory factory;

  public DocumentObject(PageObjectsFactory factory) {
    this.driver = factory.getDriver();
    this.factory = factory;
  }

  @Override
  public String getUrl() {
    return driver.getUrl();
  }

  @Override
  public void waitForDocumentReady() {
    driver.waitFor(() -> (Boolean) driver.executeScript(DOM_READY_JAVASCRIPT), "wait for DOM ready state", null);
  }

  @Override
  public boolean containsElement(Locator locator) {
    return driver.findElements(locator, NULLABLE).size() > 0;
  }

  @Override
  public boolean containsObject(Class<? extends RootPageObject> pageObjectType) {
    RootPageObject instance = factory.getPageContext().getBean(pageObjectType);
    Locator rootLocator = getRootLocator(instance);
    return driver.findElements(rootLocator, NULLABLE).size() > 0;
  }

  @Override
  public void enterFrame(FrameElement frame) {
    if(frame == null) {
      throw new UtamCoreError(ERR_CANT_ENTER_NULL_FRAME);
    }
    driver.enterFrame(getUnwrappedElement(frame));
  }

  @Override
  public void exitToParentFrame() {
    driver.exitToParentFrame();
  }

  @Override
  public void exitFrame() {
    driver.exitFrame();
  }

  @Override
  public <T extends RootPageObject> T enterFrameAndLoad(FrameElement frame, Class<T> type) {
    enterFrame(frame);
    T instance = factory.create(type);
    instance.load();
    return instance;
  }

  @Override
  public final <T> T waitFor(Supplier<T> condition) {
    return driver.waitFor(condition, "wait for condition for Document object", null);
  }
}
