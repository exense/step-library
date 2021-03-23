package ch.exense.step.examples.selenium.keyword;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ch.exense.step.examples.selenium.helper.AbstractPageObject;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import step.core.accessors.Attribute;
import step.handlers.javahandler.Keyword;

/**
 * Class containing generic selenium keywords
 */
@Attribute(key = "category",value = "Selenium")
public class GenericSeleniumKeyword extends AbstractSeleniumKeyword {

	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_INPUTS + ","
			+ "\"Keys\": {\"type\": \"string\"}"
			+ "}, \"required\" : [\"Keys\"]}", properties = { "" })
	public void Send_Keys() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		long timeout = getTimeoutFromInput();
		By element = getElementFromInput();

		String keys = input.getString("Keys");

		Map<String, Object> additionalTransactionProperties = new HashMap<>();
		additionalTransactionProperties.put("Element",element.toString());
		additionalTransactionProperties.put("Keys", keys);
		startTransaction();
		try {
			page.safeSendKeys(element, keys, timeout);
			waitForElement(page,timeout);
		} finally {
			stopTransaction(additionalTransactionProperties);
		}	
	}

	/**
	 * <p>Generic keyword used to click on the located by the locator given as input</p>
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#safeClick(By)
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#safeWait(java.util.function.Supplier, long)
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#findBy(By)
	 */
	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_INPUTS+ ","
			+ "\"AsJavascript\": {\"type\": \"boolean\"}"
			+ "}, \"required\" : []}", properties = { "" })
	public void Click() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		long timeout = getTimeoutFromInput();
		By element = getElementFromInput();
		boolean javascript = input.getBoolean("AsJavascript",false);

		Map<String, Object> additionalTransactionProperties = new HashMap<>();
		additionalTransactionProperties.put("Element",element.toString());

		startTransaction();
		try {
			if (javascript) {
				page.javascriptClick(element);
			} else {
				page.safeClick(element);
			}
			waitForElement(page,timeout);
		} finally {
			stopTransaction(additionalTransactionProperties);
		}	
	}

	/**
	 * <p>Generic keyword used to click on the located by the locator given as input</p>
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#safeClick(By)
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#safeWait(java.util.function.Supplier, long)
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#findBy(By)
	 */
	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_INPUTS+ ","
			+ "\"AsJavascript\": {\"type\": \"boolean\"}"
			+ "}, \"required\" : []}", properties = { "" })
	public void Double_Click() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		long timeout = getTimeoutFromInput();
		By element = getElementFromInput();
		boolean javascript = input.getBoolean("AsJavascript",false);

		Map<String, Object> additionalTransactionProperties = new HashMap<>();
		additionalTransactionProperties.put("Element",element.toString());

		startTransaction();
		try {
			if (javascript) {
				page.javascriptDoubleClick(element);
			} else {
				page.safeDoubleClick(element,timeout);
			}
			waitForElement(page,timeout);
		} finally {
			stopTransaction(additionalTransactionProperties);
		}
	}

	/**
	 * <p>Generic keyword used to hover on the element located by the xpath given as input</p>
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
	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_INPUTS
			+ "}, \"required\" : []}", properties = { "" })
	public void Hover() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		long timeout = getTimeoutFromInput();
		By element = getElementFromInput();

		Map<String, Object> additionalTransactionProperties = new HashMap<>();
		additionalTransactionProperties.put("Element",element.toString());

		startTransaction();
		try {
			page.safeHover(element);
			waitForElement(page,timeout);
		} finally {
			stopTransaction(additionalTransactionProperties);
		}
	}

	@Keyword (schema = "{ \"properties\": { "
			+ "\"Javascript_To_Execute\": {\"type\": \"string\"},"
			+ SELENIUM_DEFAULT_WAIT_FOR_INPUTS + ","
			+ SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
			+ SELENIUM_DEFAULT_ACTION_NAME_INPUT
			+ "}, \"required\" : [\"Javascript_To_Execute\"]}", properties = { "" })
	public void Execute_Javascript() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String javascriptToExecute = input.getString("Javascript_To_Execute");
		long timeout = getTimeoutFromInput();

		startTransaction();
		try {
			page.executeJavascript(javascriptToExecute);
			waitForElement(page,timeout);
		} finally {
			stopTransaction();
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
	@Keyword (schema = "{ \"properties\": { "
			+ "\"Optional\": {\"type\": \"boolean\"},"
			+ SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
			+ SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
			+ SELENIUM_DEFAULT_ACTION_NAME_INPUT
			+ "}, \"required\" : []}", properties = { "" })
	public void Is_Displayed() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		long timeout = getTimeoutFromInput();
		By element = getElementFromInput();
		boolean optional = input.containsKey("Optional") ? input.getBoolean("Optional") : false;

		Map<String, Object> additionalTransactionProperties = new HashMap<>();
		additionalTransactionProperties.put("Element",element.toString());

		startTransaction();
		try {
			page.safeWait(() -> page.findBy(element, timeout).isDisplayed());
			output.add("Exists", true);
		} catch(NoSuchElementException e) {
			if(optional)
				output.add("Exists", false);
			else
				throw e;
		} finally {
			stopTransaction(additionalTransactionProperties);
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
	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
			+ SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
			+ SELENIUM_DEFAULT_ACTION_NAME_INPUT
			+ "}, \"required\" : []}", properties = { "" })
	public void Get_Text() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		long timeout = getTimeoutFromInput();
		By element = getElementFromInput();

		Map<String, Object> additionalTransactionProperties = new HashMap<>();
		additionalTransactionProperties.put("Element",element.toString());

		startTransaction();
		try {
			page.safeWait(() -> {
				String text = page.findBy(element, timeout).getText();
				output.add("Text", text);
				return true;
			});
		} finally {
			stopTransaction(additionalTransactionProperties);
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
	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
			+ SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
			+ SELENIUM_DEFAULT_ACTION_NAME_INPUT
			+ "}, \"required\" : []}", properties = { "" })
	public void Enter_Iframe() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		long timeout = getTimeoutFromInput();
		By element = getElementFromInput();

		Map<String, Object> additionalTransactionProperties = new HashMap<>();
		additionalTransactionProperties.put("Element",element.toString());

		startTransaction();
		try {
			page.safeWait(() -> {
				page.waitForFrame(element); // This will also do the switch
				return true;
			},timeout);
		} finally {
			stopTransaction(additionalTransactionProperties);
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
	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_ACTION_NAME_INPUT
			+ "}, \"required\" : []}", properties = { "" })
	public void Exit_Iframe() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);

		startTransaction();
		try {
			page.switchToDefaultContent();
		} finally {
			stopTransaction();
		}
	}

	/**
	 * <p>Generic keyword used to select options from comboboxes by the xpath given as input.</p>
	 * <p>Only one method of selection can be used, in order by priority index, value or text.</p>
	 * Inputs (default values):
	 * <ul>
	 * <li>select_tag_xpath (): the element xpath to the combobox
	 * <li>index (): optional index to select
	 * <li>value (): optional value to select
	 * <li>text (): optional visible text to select
	 * <li>timeout (): optional time to wait in seconds for the element xpath to be checked
	 * </ul>
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#safeClick(By)
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#safeWait(java.util.function.Supplier, long)
	 * @see ch.exense.step.examples.selenium.helper.AbstractPageObject#findBy(By)
	 */
	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
			+ SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
			+ SELENIUM_DEFAULT_ACTION_NAME_INPUT + ","
			+ "\"Index\": {\"type\": \"integer\"},"
			+ "\"Value\": {\"type\": \"string\"},"
			+ "\"Text\": {\"type\": \"string\"}"
			+ "}, \"required\" : []}", properties = { "" })
	public void Select_Option() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		long timeout = getTimeoutFromInput();
		By element = getElementFromInput();

		Map<String, Object> additionalTransactionProperties = new HashMap<>();
		additionalTransactionProperties.put("Element",element.toString());

		Integer index = input.getInt("Index");
		String value = input.getString("Value");
		String text = input.getString("Text");

		startTransaction();
		try {
			page.safeWait(() -> {
				WebElement obj = page.findBy(element);
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
			stopTransaction(additionalTransactionProperties);
		}
	}

	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_ACTION_NAME_INPUT + ","
			+ "\"Handle\": {\"type\": \"string\"}"
			+ "}, \"required\" : [\"Handle\"]}", properties = { "" })
	public void Select_Window() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);
		String handle = input.getString("Handle","");

		startTransaction();
		try {
			output.add("Handles", StringUtils.join(page.getDriver().getWindowHandles(),","));
			page.switchToWindow(handle);
		} finally {
			stopTransaction();
		}
	}

	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_ACTION_NAME_INPUT
			+ "}, \"required\" : []}", properties = { "" })
	public void Get_Window_Handles() {
		AbstractPageObject page = getPageObject(AbstractPageObject.class);

		startTransaction();
		try {
			String main = page.getDriver().getWindowHandle();
			HashSet<String> handles = new HashSet(page.getDriver().getWindowHandles());
			handles.remove(main);

			output.add("main", main);
			output.add("popups", StringUtils.join(handles, ","));
		} finally {
			stopTransaction();
		}
	}

	/**
	 * <p>Keyword used to set the scroll top position of any web element
	 * Inputs (default values):
	 * <ul>
	 * <li>xpath of the element to scroll</li>
	 * <li>scrollTop value to be applied (0 is top, large value fall back to max. i.e. end of the element)
	 * </ul>
	 */
	@Keyword (schema = "{ \"properties\": { "
			+ SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
			+ SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
			+ SELENIUM_DEFAULT_ACTION_NAME_INPUT + ","
			+ "\"scrollTop\": {\"type\": \"string\"}"
			+ "}, \"required\" : [\"xpath\"]}", properties = { "" })
	public void Set_ScrollTop() {
		long timeout = getTimeoutFromInput();
		By element = getElementFromInput();

		JavascriptExecutor jse = (JavascriptExecutor) this.getDriver();

		int scrollTop = Integer.parseInt(input.getString("scrollTop", "0"));

		startTransaction();
		jse.executeScript("arguments[0].scrollTop=arguments[1];", element, scrollTop);
		stopTransaction();
	}
}
