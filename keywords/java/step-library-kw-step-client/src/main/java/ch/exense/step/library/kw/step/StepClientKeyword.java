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
package ch.exense.step.library.kw.step;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import ch.exense.step.library.commons.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.client.StepClient;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.repositories.RepositoryObjectReference;
import step.handlers.javahandler.Keyword;

public class StepClientKeyword extends AbstractEnhancedKeyword {

	private static final String DEFAULT_USER = "admin";
	private static final String DEFAULT_PASSWORD = "init";
	
	private static final String DEFAULT_TIMEOUT = Integer.toString(60*1000);

	@Keyword(schema = "{\"properties\":{" 
				+ "\"User\":{\"type\":\"string\"}," 
				+ "\"Password\":{\"type\":\"string\"},"
				+ "\"Url\":{\"type\":\"string\"}" 
				+ "},\"required\":[\"Url\"]}", 
			properties = { "" })
	public void InitStepClient() throws BusinessException {
		
		String url = getMandatoryInputString("Url");
		String user = input.getString("User",DEFAULT_USER);
		String password = input.getString("Password",DEFAULT_PASSWORD);
		
		getSession().put(new StepClient(url,user,password));
		getSession().put("User",user);
	}

	@Keyword(schema = "{\"properties\":{" 
				+ "\"RepositoryID\":{\"type\":\"string\"}," 
				+ "\"RepositoryParameters\":{\"type\":\"string\"},"
				+ "\"Description\":{\"type\":\"string\"},"
				+ "\"CustomParameters\":{\"type\":\"string\"},"
				+ "\"Timeout\":{\"type\":\"string\"}"
				+ "},\"required\":[\"RepositoryID\",\"RepositoryParameters\",\"Description\",\"CustomParameters\"]}", 
			properties = { "" })
	public void RunExecution() throws BusinessException, JsonMappingException, JsonProcessingException, InterruptedException {
		
		StepClient client = getSession().get(StepClient.class); 
		
		String repoId = getMandatoryInputString("RepositoryID");
		String repoParametersJson = getMandatoryInputString("RepositoryParameters");
		String description = getMandatoryInputString("Description");
		String customParametersJson = getMandatoryInputString("CustomParameters");
		
		long timeout = Long.parseLong(input.getString("Timeout", DEFAULT_TIMEOUT));
		
		ExecutionParameters executionParams = new ExecutionParameters();

		executionParams.setMode(ExecutionMode.RUN);
		executionParams.setUserID((String) getSession().get("User"));
		
		RepositoryObjectReference repoObject = new RepositoryObjectReference();
		
		executionParams.setDescription(description);
		
		repoObject.setRepositoryID(repoId);
		
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> repoParameters = mapper.readValue(repoParametersJson, new TypeReference<Map<String, String>>() {});
		
		repoObject.setRepositoryParameters(repoParameters);
		executionParams.setRepositoryObject(repoObject);
		
		Map<String, String> customParameters = new HashMap<String, String>();

		customParameters = mapper.readValue(customParametersJson, new TypeReference<Map<String, String>>() {});
		
		executionParams.setCustomParameters(customParameters);
		
		String executionID = client.getExecutionManager().execute(executionParams);
		
		output.add("Id", executionID);
		
		Execution exec;
		try {
			exec = client.getExecutionManager().waitForTermination(executionID, timeout);
		} catch (TimeoutException e) {
			output.setBusinessError("Execution '"+executionID+"' did not terminate before the timeout of "+timeout+"ms");
			return;
		}

		if (exec.getImportResult().isSuccessful()) {
			output.add("Result",exec.getResult().toString());
		} else {
			output.add("Result","IMPORT_ERROR");
			output.add("Import_Error",String.join(",", exec.getImportResult().getErrors()));
		}
	}

	private String getMandatoryInputString(String inputName) throws BusinessException {
		if (!input.containsKey(inputName)) {
			throw new BusinessException("The mandatory input parameter '"+inputName+"' is missing");
		} else {
			return input.getString(inputName);
		}
	}
}