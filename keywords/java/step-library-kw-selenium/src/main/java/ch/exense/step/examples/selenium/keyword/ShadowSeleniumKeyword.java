package ch.exense.step.examples.selenium.keyword;

import ch.exense.step.library.commons.BusinessException;
import ch.exense.step.library.selenium.AbstractPageObject;
import ch.exense.step.library.selenium.AbstractSeleniumKeyword;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import step.handlers.javahandler.Keyword;

import javax.json.JsonNumber;
import javax.json.JsonString;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Class containing selenium keywords to interact with Shadow DOM element via CSS Selectors
 */
public class ShadowSeleniumKeyword extends AbstractSeleniumKeyword {
    private static final String INPUT_SHADOW_SELECTORS = "Shadow_Selectors";

    protected static final String SELENIUM_DEFAULT_ELEMENT_INPUTS = ""
            +"\""+INPUT_SHADOW_SELECTORS+"\": {\"type\": \"string\"}";

    protected static final String SELENIUM_DEFAULT_WAIT_FOR_INPUTS = ""
            +"\""+INPUT_WAIT_FOR_PREFIX+INPUT_SHADOW_SELECTORS+"\": {\"type\": \"string\"}";

    /**
     * <p>Generic keyword used to send keys to an input element embedded into Shadow DOM element(s)</p>
     * Inputs (default values):
     * <ul>
     * <li>Shadow_Selectors(): comma separated string of CSS Selectors to reach the element, the comma separation representing a shadow-root element
     * <li>Keys(): the keys to be sent
     * <li>Timeout(): optional time to wait in seconds for the element to be found
     * </ul>
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeWait(java.util.function.Supplier, long)
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeSendKeys(String[], String, long)
     */
    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_INPUTS + ","
            + "\"Keys\": {\"type\": \"string\"}"  + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : [\"Keys\"]}", properties = { "" },
            description = "Keyword used to send keys to an input element embedded into Shadow DOM element(s).")
    public void Shadow_Send_Keys() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String selectors = getSelectorsFromInput();
        String keys = input.getString("Keys");

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        additionalTransactionProperties.put("Selectors", selectors);
        additionalTransactionProperties.put("Keys", keys);
        startTransaction();
        try {
            page.safeSendKeys(selectors.split(","), keys, timeout);
            waitForElement(page,timeout);
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    /**
     * <p>Generic keyword used to select options from combo boxes embedded into Shadow DOM element(s) as input.</p>
     * <p>Only one method of selection can be used, in order by priority index, value or text.</p>
     * Inputs (default values):
     * <ul>
     * <li>Shadow_Selectors(): comma separated string of CSS Selectors to reach the element, the comma separation representing a shadow-root element
     * <li>Index(): optional index to select
     * <li>Value(): optional value to select
     * <li>Text(): optional visible text to select
     * <li>Timeout(): optional time to wait in seconds for the element to be found
     * </ul>
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeWait(java.util.function.Supplier, long)
     * @see ch.exense.step.library.selenium.AbstractPageObject#expandShadowPath(long, String[]...)
     */
    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT + ","
            + "\"Index\": {\"type\": \"integer\"},"
            + "\"Value\": {\"type\": \"string\"},"
            + "\"Text\": {\"type\": \"string\"}"
            + "}, \"required\" : []}", properties = { "" },
            description = "Keyword used to select options from combo boxes embedded into Shadow DOM element(s).")
    public void Shadow_Select_Option() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String selectors = getSelectorsFromInput();

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        additionalTransactionProperties.put("Selectors", selectors);

        JsonNumber index = input.getJsonNumber("Index");
        JsonString value = input.getJsonString("Value");
        JsonString text = input.getJsonString("Text");

        startTransaction();
        try {
            page.safeWait(() -> {
                WebElement obj = page.expandShadowPath(timeout, selectors.split(","));
                Select sel = new Select(obj);
                if(index != null)
                    sel.selectByIndex(index.intValue());
                else if(value != null)
                    sel.selectByValue(value.toString());
                else if(text != null)
                    sel.selectByVisibleText(text.toString());
                return true;
            }, timeout);
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    /**
     * <p>Keyword used to click on an element embedded into Shadow DOM element(s)</p>
     * Inputs (default values):
     * <ul>
     * <li>Shadow_Selectors(): comma separated string of CSS Selectors to reach the element, the comma separation representing a shadow-root element
     * <li>AsJavascript(false): boolean, if true the click will be performed using Javascript </li>
     * <li>Timeout(): optional time to wait in seconds for the element to be found
     * </ul>
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeClick(String[])
     * @see ch.exense.step.library.selenium.AbstractPageObject#javascriptClick(String[])
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeWait(java.util.function.Supplier, long)
     */
    @Keyword(schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_INPUTS+ ","
            + "\"AsJavascript\": {\"type\": \"boolean\"}"  + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : []}", properties = { "" },
            description = "Keyword used to click on an element embedded into Shadow DOM element(s).")
    public void Shadow_Click() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String selectors = getSelectorsFromInput();
        boolean javascript = input.getBoolean("AsJavascript",false);

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        additionalTransactionProperties.put("Selectors", selectors);

        startTransaction();
        try {
            if (javascript) {
                page.javascriptClick(selectors.split(","));
            } else {
                page.safeClick(selectors.split(","), timeout);
            }
            waitForElement(page,timeout);
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    /**
     * <p>Generic keyword used to double click on an element embedded into Shadow DOM element(s)</p>
     * Inputs (default values):
     * <ul>
     * <li>Shadow_Selectors(): comma separated string of CSS Selectors to reach the element, the comma separation representing a shadow-root element
     * <li>AsJavascript(false): boolean, if true the click will be performed using Javascript </li>
     * <li>Timeout(): optional time to wait in seconds for the element to be found
     * </ul>
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeDoubleClick(String[], long)
     * @see ch.exense.step.library.selenium.AbstractPageObject#javascriptDoubleClick(String[])
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeWait(java.util.function.Supplier, long)
     */
    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_INPUTS+ ","
            + "\"AsJavascript\": {\"type\": \"boolean\"}"  + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : []}", properties = { "" },
            description = "Keyword used to double-click on an element embedded into Shadow DOM element(s).")
    public void Shadow_Double_Click() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String selectors = getSelectorsFromInput();
        boolean javascript = input.getBoolean("AsJavascript",false);

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        additionalTransactionProperties.put("Selectors", selectors);

        startTransaction();
        try {
            if (javascript) {
                page.javascriptDoubleClick(selectors.split(","));
            } else {
                page.safeDoubleClick(selectors.split(","),timeout);
            }
            waitForElement(page,timeout);
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    /**
     * <p>Generic keyword used to get text from an element embedded into Shadow DOM element(s).</p>
     * Inputs (default values):
     * <ul>
     * <li>Shadow_Selectors(): comma separated string of CSS Selectors to reach the element, the comma separation representing a shadow-root element
     * <li>Timeout(): optional time to wait in seconds for the element to be found
     * </ul>
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeWait(Supplier)
     */
    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : []}", properties = { "" },
            description = "Keyword used to get the text of an element embedded into Shadow DOM element(s).")
    public void Shadow_Get_Text() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String selectors = getSelectorsFromInput();

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        additionalTransactionProperties.put("Selectors", selectors);

        startTransaction();
        try {
            page.safeWait(() -> {
                String text = page.expandShadowPath(timeout, selectors.split(",")).getText();
                output.add("Text", text);
                return true;
            });
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    /**
     * <p>Generic keyword used to wait until an element embedded into Shadow DOM element(s) is displayed on the page.</p>
     * Inputs (default values):
     * <ul>
     * <li>Shadow_Selectors(): comma separated string of CSS Selectors to reach the element, the comma separation representing a shadow-root element
     * <li>Timeout(): optional time to wait in seconds for the element to be found
     * <li>Optional(false): boolean, if set to true the keyword will fail is the element is not displayed</li>
     * </ul>
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeWait(Supplier, long) 
     */
    @Keyword (schema = "{ \"properties\": { "
            + "\"Optional\": {\"type\": \"boolean\"},"
            + SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : []}", properties = { "" },
            description = "Keyword used to wait until an element embedded into Shadow DOM element(s) is displayed on the page.")
    public void Shadow_Is_Displayed() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String selectors = getSelectorsFromInput();
        boolean optional = input.containsKey("Optional") && input.getBoolean("Optional");

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        additionalTransactionProperties.put("Selectors", selectors);

        startTransaction();
        try {
            page.safeWait(() -> page.expandShadowPath(timeout, selectors.split(",")).isDisplayed(), timeout);
            output.add("Exists", true);
        } catch(RuntimeException e) {
            if(optional && (e instanceof NoSuchElementException ||
                    e.getMessage().equals("Timeout while waiting for condition to apply.")))
                output.add("Exists", false);
            else
                throw e;
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    /**
     * <p>Generic keyword used to hover on an element embedded into Shadow DOM element(s)</p>
     * Inputs (default values):
     * <ul>
     * <li>Shadow_Selectors(): comma separated string of CSS Selectors to reach the element, the comma separation representing a shadow-root element
     * <li>Timeout(): optional time to wait in seconds for the element to be found
     * </ul>
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeHover(String[])
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeWait(Supplier)
     */
    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_INPUTS + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : []}", properties = { "" },
            description = "Keyword used to hover on an element embedded into Shadow DOM element(s).")
    public void Shadow_Hover() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String selectors = getSelectorsFromInput();

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        additionalTransactionProperties.put("Selectors", selectors);

        startTransaction();
        try {
            page.safeHover(selectors.split(","));
            waitForElement(page,timeout);
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    /**
     * <p>Keyword used to set the scroll to an element embedded into Shadow DOM element(s)
     * Inputs (default values):
     * <ul>
     * <li>Shadow_Selectors(): comma separated string of CSS Selectors to reach the element, the comma separation representing a shadow-root element
     * </ul>
     * @see ch.exense.step.library.selenium.AbstractPageObject#expandShadowPath(String[]...)
     */
    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT + ","
            + "\"ScrollTop\": {\"type\": \"string\"}"
            + "}, \"required\" : [\"Shadow_Selectors\"]}", properties = { "" },
            description = "Keyword used to scroll to an element embedded into Shadow DOM element(s).")
    public void Shadow_Set_ScrollIntoView() {
        String selectors = getSelectorsFromInput();
        AbstractPageObject page = getPageObject();
        WebElement obj = page.expandShadowPath(selectors.split(","));

        JavascriptExecutor jse = (JavascriptExecutor) this.getDriver();

        startTransaction();
        jse.executeScript("arguments[0].scrollIntoView(true);", obj);
        stopTransaction();
    }

    /**
     * <p>Generic keyword used to enter iframe selected by xpath.</p>
     * Inputs (default values):
     * <ul>
     * <li>Xpath(): the path to the element to wait for
     * <li>Timeout(): optional time to wait in seconds for the element xpath to be checked
     * </ul>
     * @see AbstractPageObject#waitForFrameAndSwitchDriver(By)
     */
    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : []}", properties = { "" },
            description = "Keyword used to enter an iframe into Shadow DOM element(s).")
    public void Shadow_Enter_Iframe() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String selectors = getSelectorsFromInput();

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        additionalTransactionProperties.put("Selectors", selectors);

        startTransaction();
        try {
            page.safeWait(() -> {
                page.waitForFrame(page.expandShadowPath(timeout, selectors.split(","))); // This will also do the switch
                return true;
            },timeout);
        } finally {
            stopTransaction(additionalTransactionProperties);
        }

    }

    private String getSelectorsFromInput() {
        return getSelectorsFromInput("", true);
    }

    private String getSelectorsFromInput(String prefix, boolean errorIfNotFound) {
        if(input.containsKey(prefix+INPUT_SHADOW_SELECTORS)) {
            return input.getString(prefix+INPUT_SHADOW_SELECTORS);
        } else if(errorIfNotFound) {
            throw new BusinessException("Error: could not get the selectors from the input");
        }
        return null;
    }

    protected void waitForElement(AbstractPageObject page, long timeout) {
        String selectors = getSelectorsFromInput(INPUT_WAIT_FOR_PREFIX, false);
        if(selectors != null) {
            page.safeWait(() -> page.expandShadowPath(timeout, selectors.split(",")).isDisplayed(), timeout);
        }
    }
}