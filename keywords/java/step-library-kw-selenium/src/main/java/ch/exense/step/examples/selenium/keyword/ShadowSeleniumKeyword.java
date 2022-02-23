package ch.exense.step.examples.selenium.keyword;

import ch.exense.step.library.selenium.AbstractPageObject;
import ch.exense.step.library.selenium.AbstractSeleniumKeyword;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import step.core.accessors.Attribute;
import step.handlers.javahandler.Keyword;

import java.util.HashMap;
import java.util.Map;

/**
 * Class containing selenium keywords to interact with Shadow DOM element via CSS Selectors
 */
@Attribute(key = "category",value = "Selenium")
public class ShadowSeleniumKeyword extends AbstractSeleniumKeyword {
    private static final String INPUT_SHADOW_SELECTORS = "Shadow_Selectors";

    protected static final String SELENIUM_DEFAULT_ELEMENT_INPUTS = ""
            +"\""+INPUT_SHADOW_SELECTORS+"\": {\"type\": \"string\"}";

    protected static final String SELENIUM_DEFAULT_WAIT_FOR_INPUTS = ""
            +"\""+INPUT_WAIT_FOR_PREFIX+INPUT_SHADOW_SELECTORS+"\": {\"type\": \"string\"}";

    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_INPUTS + ","
            + "\"Keys\": {\"type\": \"string\"}"  + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : [\"Keys\"]}", properties = { "" })
    public void Shadow_Send_Keys() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String[] selectors = getSelectorsFromInput();

        String keys = input.getString("Keys");

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        //additionalTransactionProperties.put("Element",element.toString());
        additionalTransactionProperties.put("Keys", keys);
        startTransaction();
        try {
            page.safeSendKeys(selectors, keys, timeout);
            waitForElement(page,timeout);
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    /**
     * <p>Keyword used to click on an element embedded into Shadow DOM element(s)</p>
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeClick(By)
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeWait(java.util.function.Supplier, long)
     * @see ch.exense.step.library.selenium.AbstractPageObject#findBy(By)
     */
    @Keyword(schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_INPUTS+ ","
            + "\"AsJavascript\": {\"type\": \"boolean\"}"  + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : []}", properties = { "" })
    public void Shadow_Click() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String[] selectors = getSelectorsFromInput();
        boolean javascript = input.getBoolean("AsJavascript",false);

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        //additionalTransactionProperties.put("Element",element.toString());

        startTransaction();
        try {
            if (javascript) {
                page.javascriptClick(selectors);
            } else {
                page.safeClick(selectors);
            }
            waitForElement(page,timeout);
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    /**
     * <p>Generic keyword used to click on the element located by the locator given as input</p>
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeClick(By)
     * @see ch.exense.step.library.selenium.AbstractPageObject#safeWait(java.util.function.Supplier, long)
     * @see ch.exense.step.library.selenium.AbstractPageObject#findBy(By)
     */
    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_INPUTS+ ","
            + "\"AsJavascript\": {\"type\": \"boolean\"}"  + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : []}", properties = { "" })
    public void Shadow_Double_Click() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String[] selectors = getSelectorsFromInput();
        boolean javascript = input.getBoolean("AsJavascript",false);

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        //additionalTransactionProperties.put("Element",element.toString());

        startTransaction();
        try {
            if (javascript) {
                page.javascriptDoubleClick(selectors);
            } else {
                page.safeDoubleClick(selectors,timeout);
            }
            waitForElement(page,timeout);
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    /**
     * <p>Generic keyword used to get text from the element displayed on the page.</p>
     * Inputs (default values):
     * <ul>
     * <li>Xpath(): the path to the element to wait for
     * <li>Timeout(): optional time to wait in seconds for the element xpath to be checked
     * </ul>
     * Outputs:
     * <ul>
     * <li>Text : text of the element
     * </ul>
     * @see ch.exense.step.library.selenium.AbstractPageObject#findBy(By)
     */
    @Keyword (schema = "{ \"properties\": { "
            + SELENIUM_DEFAULT_ELEMENT_INPUTS + ","
            + SELENIUM_DEFAULT_TIMEOUT_INPUT + ","
            + SELENIUM_DEFAULT_ACTION_NAME_INPUT
            + "}, \"required\" : []}", properties = { "" })
    public void Shadow_Get_Text() {
        AbstractPageObject page = getPageObject();
        long timeout = getTimeoutFromInput();
        String[] selectors = getSelectorsFromInput();

        Map<String, Object> additionalTransactionProperties = new HashMap<>();
        //additionalTransactionProperties.put("Element",element.toString());

        startTransaction();
        try {
            page.safeWait(() -> {
                String text = page.expandShadowPath(timeout, selectors).getText();
                output.add("Text", text);
                return true;
            });
        } finally {
            stopTransaction(additionalTransactionProperties);
        }
    }

    private String[] getSelectorsFromInput() {
        return input.getString(INPUT_SHADOW_SELECTORS).split(",");
    }

    protected void waitForElement(AbstractPageObject page, long timeout) {
        if(input.containsKey(INPUT_WAIT_FOR_PREFIX+INPUT_SHADOW_SELECTORS)) {
            String inputSelectors = input.getString(INPUT_WAIT_FOR_PREFIX+INPUT_SHADOW_SELECTORS);
            if(inputSelectors != null) {
                String[] selectors = inputSelectors.split(",");
                if(selectors.length > 0) {
                    page.safeWait(() -> page.expandShadowPath(selectors).isDisplayed(), timeout);
                }
            }
        }
    }
}
