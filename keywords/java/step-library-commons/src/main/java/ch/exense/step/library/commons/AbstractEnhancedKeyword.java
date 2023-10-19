/*******************************************************************************
 * Copyright 2021 exense GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package ch.exense.step.library.commons;

import step.handlers.javahandler.AbstractKeyword;

/**
 * An Enhanced Abstract keyword using the onError function for
 * managing business errors via exceptions
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

	protected String getPassword(String username) {
		if (!properties.containsKey(username + "_Password")) {
			throw new BusinessException(String.format("No password found for user '%s'. " +
							"Please define the following protected parameters: '%s_Password'",username, username));
		}
		return properties.get(username + "_Password");
	}
}
