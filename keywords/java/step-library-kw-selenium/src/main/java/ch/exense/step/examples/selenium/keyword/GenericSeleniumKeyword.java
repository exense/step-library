package ch.exense.step.examples.selenium.keyword;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ch.exense.step.examples.selenium.helper.AbstractPageObject;
import ch.exense.step.examples.selenium.helper.AbstractPageObject;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import step.handlers.javahandler.Keyword;

/**
 * Class containing generic selenium keywords
 * @author rubieroj
 */
public class GenericSeleniumKeyword extends SeleniumKeyword {
	
	@Keyword
	public void Send_keys_by_xpath() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String xpath = input.getString("xpath");
		String elementXPathToCheckIfDisplayed = input.getString("elementXPathToCheckIfDisplayed", "");
		String keys = input.getString("keys");
		
		Map<String, Object> additionalTransactionProperties = new HashMap<>();
		additionalTransactionProperties.put("xpath", xpath);
		additionalTransactionProperties.put("keys", keys);
		
		long timeout = input.containsKey("timeout") ? input.getInt("timeout") : AbstractPageObject.getDefaultTimeout();
		startTransaction("Send_keys_by_xpath");
		try {
			page.safeSendKeys(By.xpath(xpath), keys);
			if(elementXPathToCheckIfDisplayed != null && !elementXPathToCheckIfDisplayed.isEmpty()) {
				page.safeWait(() -> {
					return page.findBy(By.xpath(elementXPathToCheckIfDisplayed)).isDisplayed();
				}, timeout);
			}
		} finally {
			stopTransaction("Send_keys_by_xpath", additionalTransactionProperties);
		}	
	}
	
	/**
	 * <p>Generic keyword used to click on the located by the xpath given as input</p>
	 * Inputs (default values):
	 * <ul>
	 * <li>xpath (): the element xpath to click on
	 * <li>elementXPathToCheckIfDisplayed (): optional element xpath to check if displayed after clicking
	 * <li>timeout (): optional time to wait in seconds for the element xpath to be checked
	 * </ul>
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#safeClick(By)
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#safeWait(java.util.function.Supplier, long)
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#findBy(By)
	 */
	@Keyword
	public void Click_by_xpath() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String xpath = input.getString("xpath");
		String elementXPathToCheckIfDisplayed = input.getString("elementXPathToCheckIfDisplayed", "");
		long timeout = input.containsKey("timeout") ? input.getInt("timeout") : AbstractPageObject.getDefaultTimeout();
		startTransaction("Click_by_xpath");
		try {
			page.safeClick(By.xpath(xpath));
			if(elementXPathToCheckIfDisplayed != null && !elementXPathToCheckIfDisplayed.isEmpty()) {
				page.safeWait(() -> {
					return page.findBy(By.xpath(elementXPathToCheckIfDisplayed)).isDisplayed();
				}, timeout);
			}
		} finally {
			stopTransaction("Click_by_xpath", "xpath", xpath);
		}	
	}
	
	@Keyword
	public void Execute_javascript() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String javascriptToExecute = input.getString("javascriptToExecute");
		String elementXPathToCheckIfDisplayed = input.getString("elementXPathToCheckIfDisplayed", "");
		long timeout = input.containsKey("timeout") ? input.getInt("timeout") : AbstractPageObject.getDefaultTimeout();
		startTransaction("Click_by_xpath_javascript");
		try {
			page.executeJavascript(javascriptToExecute);
			if(elementXPathToCheckIfDisplayed != null && !elementXPathToCheckIfDisplayed.isEmpty()) {
				page.safeWait(() -> {
					return page.findBy(By.xpath(elementXPathToCheckIfDisplayed)).isDisplayed();
				}, timeout);
			}
		} finally {
			stopTransaction("Execute_javascript", "javascriptToExecute", javascriptToExecute);
		}
	}
	
	@Keyword
	public void Click_by_xpath_javascript() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String xpath = input.getString("xpath");
		String elementXPathToCheckIfDisplayed = input.getString("elementXPathToCheckIfDisplayed", "");
		long timeout = input.containsKey("timeout") ? input.getInt("timeout") : AbstractPageObject.getDefaultTimeout();
		startTransaction("Click_by_xpath_javascript");
		try {
			page.javascriptClick(xpath);
			if(elementXPathToCheckIfDisplayed != null && !elementXPathToCheckIfDisplayed.isEmpty()) {
				page.safeWait(() -> {
					return page.findBy(By.xpath(elementXPathToCheckIfDisplayed)).isDisplayed();
				}, timeout);
			}
		} finally {
			stopTransaction("Click_by_xpath_javascript", "xpath", xpath);
		}	
	}

	/**
	 * <p>Generic keyword used to wait until an element is displyed on the page.</p>
	 * Inputs (default values):
	 * <ul>
	 * <li>xpath (): the path to the element to wait for
	 * <li>timeout (): optional time to wait in seconds for the element xpath to be checked
	 * </ul>
	 * Outputs:
	 * <ul>
	 * <li>exists : true if element exists, false otherwise
	 * </ul>
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#findBy(By)
	 */
	@Keyword 
	public void Check_element_is_displayed_by_xpath() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String xpath = input.getString("xpath");
		boolean optional = input.containsKey("optional") ? input.getBoolean("optional") : false;
		long timeout = input.containsKey("timeout") ? input.getInt("timeout") : AbstractPageObject.getDefaultTimeout();
		startTransaction("Check_element_is_displayed_by_xpath_advisor");
		try {
			page.safeWait(() -> {
				return page.findBy(By.xpath(xpath), timeout).isDisplayed();
			});
			output.add("exists", true);
		} catch(NoSuchElementException e) {
			if(optional)
				output.add("exists", false);
			else
				throw e;
		} finally {
			stopTransaction("Check_element_is_displayed_by_xpath_advisor", "xpath", xpath);
		}
	}

	/**
	 * <p>Generic keyword used to get text from element displayed on the page.</p>
	 * Inputs (default values):
	 * <ul>
	 * <li>xpath (): the path to the element to wait for
	 * <li>timeout (): optional time to wait in seconds for the element xpath to be checked
	 * </ul>
	 * Outputs:
	 * <ul>
	 * <li>text : text of the element
	 * </ul>
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#findBy(By)
	 */
	@Keyword
	public void Get_text_by_xpath() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String xpath = input.getString("xpath");
		long timeout = input.containsKey("timeout") ? input.getInt("timeout") : AbstractPageObject.getDefaultTimeout();
		startTransaction("Get_text_by_xpath");
		try {
			page.safeWait(() -> {
				String text = page.findBy(By.xpath(xpath), timeout).getText();
				output.add("text", text);
				return true;
			});
		} finally {
			stopTransaction("Get_text_by_xpath", "xpath", xpath);
		}
	}

	/**
	 * <p>Generic keyword used to enter iframe selected by xpath.</p>
	 * Inputs (default values):
	 * <ul>
	 * <li>xpath (): the path to the element to wait for
	 * <li>timeout (): optional time to wait in seconds for the element xpath to be checked
	 * </ul>
	 * @see AbstractPageObject#waitForFrameAndSwitchDriver(By)
	 */
	@Keyword
	public void Enter_iframe_by_xpath() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String xpath = input.getString("xpath");
		long timeout = input.containsKey("timeout") ? input.getInt("timeout") : AbstractPageObject.getDefaultTimeout();
		startTransaction("Enter_iframe_by_xpath");
		try {
			page.safeWait(() -> {
				page.waitForFrame(By.xpath(xpath)); // This will also do the switch
				return true;
			});
		} finally {
			stopTransaction("Enter_iframe_by_xpath", "xpath", xpath);
		}

	}

	/**
	 * <p>Generic keyword used to eit from ifame context.</p>
	 * Inputs (default values):
	 * <ul>
	 * <li>timeout (): optional time to wait in seconds for the element xpath to be checked
	 * </ul>
	 * @see AbstractPageObject#switchToDefaultContent()
	 * @see AbstractPageObject#waitForFrameAndSwitchDriver(By)
	 */
	@Keyword
	public void Exit_iframe() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		startTransaction("Exit_iframe");
		try {
			page.switchToDefaultContent();
		} finally {
			stopTransaction("Exit_iframe");
		}
	}

	/**
	 * <p>Generic keyword used to select options from comboboxes by the xpath given as input.</p>
	 * <p>Only one method of selection can be used, in order by priority index, value or text.</p>
	 * Inputs (default values):
	 * <ul>
	 * <li>xpath (): the element xpath to the combobox
	 * <li>index (): optional index to select
	 * <li>value (): optional value to select
	 * <li>text (): optional visible text to select
	 * <li>timeout (): optional time to wait in seconds for the element xpath to be checked
	 * </ul>
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#safeClick(By)
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#safeWait(java.util.function.Supplier, long)
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#findBy(By)
	 */
	@Keyword
	public void Select_option_by_xpath() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String xpath = input.getString("xpath");
		Integer index = input.getInt("index");
		String value = input.getString("value");
		String text = input.getString("text");
		long timeout = input.containsKey("timeout") ? input.getInt("timeout") : AbstractPageObject.getDefaultTimeout();
		startTransaction("Select_option_by_xpath");
		try {
			page.safeWait(() -> {
				WebElement obj = page.findBy(By.xpath(xpath));
				Select sel = new Select(obj);
				if(index != null)
					sel.selectByIndex(index);
				else if(value != null)
					sel.selectByValue(value);
				else if(text != null)
					sel.selectByVisibleText(text);

				return true;
			}, timeout);
		} finally {
			stopTransaction("Select_option_by_xpath", "xpath", xpath);
		}
	}

	@Keyword
	public void Select_window_by_handle() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String handle = input.getString("handle","");
		startTransaction("Select_window_by_handle");
		try {
			output.add("handles", StringUtils.join(page.getDriver().getWindowHandles(),","));
			page.switchToWindow(handle);
		} finally {
			stopTransaction("Select_window_by_handle", "handle", handle);
		}
	}

	@Keyword
	public void Get_window_handles() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		startTransaction("Get_window_handles");
		try {
			String main = page.getDriver().getWindowHandle();
			HashSet<String> handles = new HashSet(page.getDriver().getWindowHandles());
			handles.remove(main);

			output.add("main", main);
			output.add("popups", StringUtils.join(handles, ","));
		} finally {
			stopTransaction("Get_window_handles");
		}
	}
}
