package ch.exense.step.library.commons;

import step.handlers.javahandler.AbstractKeyword;

/**
 * @author Jonathan Rubiero
 */
public class AbstractEnhancedKeyword extends AbstractKeyword {
	
	@Override
	public boolean onError(Exception e) {
		output.add("onError_3",e.getClass().toString());
		if(e instanceof BusinessException) {
			output.setBusinessError(e.getMessage());
			return false;
		} else {
			return super.onError(e);
		}
	}
	
}
