package ch.exense.step.examples.common.helper;

import step.handlers.javahandler.AbstractKeyword;

/**
 * @author Jonathan Rubiero
 */
public class AbstractEnhancedKeyword extends AbstractKeyword {
	
	@Override
	public boolean onError(Exception e) {
		if(e instanceof BusinessException) {
			output.setBusinessError(e.getMessage());
			return false;
		} else {
			return super.onError(e);
		}
	}
	
}
