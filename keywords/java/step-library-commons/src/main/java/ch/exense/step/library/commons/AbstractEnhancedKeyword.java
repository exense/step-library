package ch.exense.step.library.commons;

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
		} else if  (e.getCause()!=null && e.getCause() instanceof BusinessException) {
			output.setBusinessError(e.getCause().getMessage());
			return false;
		} else {
			return super.onError(e);
		}
	}
	
}
