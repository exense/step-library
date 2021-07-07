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

import ch.exense.step.library.commons.AbstractEnhancedKeyword;
import ch.exense.step.library.commons.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import step.client.ControllerClientException;
import step.client.StepClient;
import step.controller.multitenancy.Tenant;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.repositories.RepositoryObjectReference;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.Keyword;
import step.resources.ResourceRevisionContent;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class StepClientKeyword extends AbstractEnhancedKeyword {

    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASSWORD = "init";

    private static final String DEFAULT_TIMEOUT = Integer.toString(60 * 1000);

    @Keyword(schema = "{\"properties\":{"
            + "\"User\":{\"type\":\"string\"},"
            + "\"Password\":{\"type\":\"string\"},"
            + "\"Url\":{\"type\":\"string\"}"
            + "},\"required\":[\"Url\"]}",
            properties = {""})
    public void InitStepClient() throws BusinessException {

        String url = getMandatoryInputString("Url");
        String user = input.getString("User", DEFAULT_USER);
        String password = input.getString("Password", DEFAULT_PASSWORD);

        StepClient client = new StepClient(url, user, password);

        // check if correctly logged in: get the current tenant:
        try {
            Tenant tenant = client.getCurrentTenant();
            output.add("Tenant", tenant.getName());
        } catch (ControllerClientException e) {
            output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
            throw new BusinessException("Could not log into the step controller");
        }

        getSession().put(client);
        getSession().put("User", user);
    }

    @Keyword(schema = "{\"properties\":{"
            + "\"TenantName\":{\"type\":\"string\"},"
            + "\"ProjectId\":{\"type\":\"string\"}"
            + "}, \"oneOf\": [{\"required\":[\"TenantName\"]},"
            + "{\"required\":[\"ProjectId\"]}]}",
            properties = {""})
    public void SelectTenant() throws BusinessException {

        StepClient client = getSession().get(StepClient.class);

        String tenantName = input.getString("TenantName", "");
        String projectId = input.getString("ProjectId", "");

        if (tenantName.isEmpty()) {
            for (Tenant tenant: client.getAvailableTenants()) {
                if (tenant.getProjectId() != null && tenant.getProjectId().equals(projectId)) {
                    try {
                        client.selectTenant(tenant.getName());
                        return;
                    } catch (Exception e) {
                        output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
                        throw new BusinessException("Exception when trying to select the tenant by id");
                    }
                }
            }
        } else {
            try {
                client.selectTenant(tenantName);
                return;
            } catch (Exception e) {
                output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
                throw new BusinessException("Exception when trying to select the tenant by name");
            }
        }
        throw new BusinessException("No tenant was found for "+
                (tenantName.isEmpty()? "project id '"+projectId+"'" : "project name '"+tenantName+"'"));
    }

    @Keyword(schema = "{\"properties\":{},\"required\":[]}",
            properties = {""})
    public void ListTenants() throws BusinessException {

        StepClient client = getSession().get(StepClient.class);
        if (client==null) {
            throw new BusinessException("Client was not initialized");
        }

        try {
            if (client.getCurrentTenant()==null) {
                throw new BusinessException("client.getCurrentTenant() is null");
            }

            output.add("CurrentTenant", client.getCurrentTenant().getName());
            String id = client.getCurrentTenant().getProjectId();
            if (id != null) {
                output.add("CurrentProjectId", id);
            }
            List<String> result = new ArrayList<>();
            client.getAvailableTenants().forEach(tenant ->
                    result.add("{\"name\":\"" + tenant.getName() + "\",\"id\":\"" + tenant.getProjectId() + "\"}")
            );
            output.add("Tenants", result.toString());
        } catch (Exception e) {
            output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
            throw new BusinessException("Exception when trying to list the tenants");
        }
    }

    @Keyword(schema = "{\"properties\":{"
            + "\"ResourceID\":{\"type\":\"string\"},"
            + "\"Destination\":{\"type\":\"string\"},"
            + "\"DeleteAfter\":{\"type\":\"boolean\"}"
            + "},\"required\":[\"ResourceID\",\"Destination\"]}",
            properties = {""})
    public void DownloadResource() throws BusinessException {

        StepClient client = getSession().get(StepClient.class);

        String resourceID = input.getString("ResourceID");
        String destination = input.getString("Destination");
        boolean deleteAfter = input.getBoolean("DeleteAfter", false);

        try {
            if (client.getResourceManager().getResourceContent(resourceID) == null) {
                throw new BusinessException("Resource id '" + resourceID + "' was not found");
            }
            ResourceRevisionContent resource = client.getResourceManager().getResourceContent(resourceID);
            File file = new File(destination);
            File resourceFile;
            if (!file.isDirectory()) {
                resourceFile = file;
            } else {
                String fileName = resource.getResourceName();
                resourceFile = new File(destination + File.separatorChar + fileName);
            }
            output.add("File", resourceFile.getAbsolutePath());
            if (!resourceFile.exists() && !resourceFile.createNewFile()) {
                throw new BusinessException("Could not create destination file \"" + resourceFile.getAbsolutePath() + "\".");
            }
            try (InputStream inputStream = resource.getResourceStream();
                 OutputStream outStream = new FileOutputStream(resourceFile)) {

                byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }

            if (deleteAfter) {
                try {
                    client.getResourceManager().deleteResource(resourceID);
                } catch (ControllerClientException e) {
                }
            }
        } catch (IOException e) {
            output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
            throw new BusinessException("IOException when trying to download the resource '" + resourceID + "'");
        }
    }

    @Keyword(schema = "{\"properties\":{"
            + "\"RepositoryID\":{\"type\":\"string\"},"
            + "\"RepositoryParameters\":{\"type\":\"string\"},"
            + "\"Description\":{\"type\":\"string\"},"
            + "\"CustomParameters\":{\"type\":\"string\"},"
            + "\"Timeout\":{\"type\":\"string\"}"
            + "},\"required\":[\"RepositoryID\",\"RepositoryParameters\",\"Description\",\"CustomParameters\"]}",
            properties = {""})
    public void RunExecution() throws BusinessException, IOException, InterruptedException {

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
        Map<String, String> repoParameters = mapper.readValue(repoParametersJson, new TypeReference<Map<String, String>>() {
        });

        repoObject.setRepositoryParameters(repoParameters);
        executionParams.setRepositoryObject(repoObject);

        Map<String, String> customParameters = mapper.readValue(customParametersJson, new TypeReference<Map<String, String>>() {
        });

        executionParams.setCustomParameters(customParameters);

        String executionID = client.getExecutionManager().execute(executionParams);

        output.add("Id", executionID);

        Execution exec;
        try {
            exec = client.getExecutionManager().waitForTermination(executionID, timeout);
        } catch (TimeoutException e) {
            output.setBusinessError("Execution '" + executionID + "' did not terminate before the timeout of " + timeout + "ms");
            return;
        }

        if (exec.getImportResult().isSuccessful()) {
            output.add("Result", exec.getResult().toString());
        } else {
            output.add("Result", "IMPORT_ERROR");
            output.add("Import_Error", String.join(",", exec.getImportResult().getErrors()));
        }
    }

    private String getMandatoryInputString(String inputName) throws BusinessException {
        if (!input.containsKey(inputName)) {
            throw new BusinessException("The mandatory input parameter '" + inputName + "' is missing");
        } else {
            return input.getString(inputName);
        }
    }
}