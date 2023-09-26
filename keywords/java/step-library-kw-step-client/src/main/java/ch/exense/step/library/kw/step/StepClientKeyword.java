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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Invocation;
import step.client.AbstractRemoteClient;
import step.client.ControllerClientException;
import step.client.StepClient;
import step.controller.multitenancy.Tenant;
import step.core.accessors.AbstractOrganizableObject;
import step.core.accessors.Attribute;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionMode;
import step.core.execution.model.ExecutionParameters;
import step.core.execution.model.ExecutionStatus;
import step.core.plans.Plan;
import step.core.repositories.RepositoryObjectReference;
import step.handlers.javahandler.Keyword;
import step.resources.Resource;
import step.resources.ResourceManager;
import step.resources.SimilarResourceExistingException;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Attribute(key = "category", value = "Step Client")
public class StepClientKeyword extends AbstractEnhancedKeyword {

    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASSWORD = "init";

    private static final String DEFAULT_TIMEOUT = Integer.toString(60 * 1000);

    @Keyword(schema = "{\"properties\":{"
            + "\"User\":{\"type\":\"string\"},"
            + "\"Password\":{\"type\":\"string\"},"
            + "\"Token\":{\"type\":\"string\"},"
            + "\"Url\":{\"type\":\"string\"}"
            + "},\"required\":[\"Url\"]}",
            properties = {""},
            description = "Keyword used to initialize a step client and place it in session.")
    public void InitStepClient() throws BusinessException {

        String url = getMandatoryInputString("Url");

        StepClient client;

        if (input.containsKey("Token")) {
            client = new StepClient(url, input.getString("Token"));
        } else {
            String user = input.getString("User", DEFAULT_USER);
            getSession().put("User", user);
            client = new StepClient(url, user,
                    input.getString("Password", DEFAULT_PASSWORD));
        }

        // check if correctly logged in: get the current tenant:
        try {
            Tenant tenant = client.getCurrentTenant();
            output.add("Tenant", tenant.getName());
        } catch (ControllerClientException e) {
            throw new BusinessException("Could not log into the step controller", e);
        }

        getSession().put(client);
    }

    @Keyword(schema = "{\"properties\":{"
            + "\"TenantName\":{\"type\":\"string\"},"
            + "\"ProjectId\":{\"type\":\"string\"}"
            + "}, \"oneOf\": [{\"required\":[\"TenantName\"]},"
            + "{\"required\":[\"ProjectId\"]}]}",
            properties = {""},
            description = "Keyword used to select another tenant/project.")
    public void SelectTenant() throws BusinessException {

        StepClient client = getClient();

        String tenantName = input.getString("TenantName", "");
        String projectId = input.getString("ProjectId", "");

        if (tenantName.isEmpty()) {
            for (Tenant tenant : client.getAvailableTenants()) {
                if (tenant.getProjectId() != null && tenant.getProjectId().equals(projectId)) {
                    try {
                        client.selectTenant(tenant.getName());
                        return;
                    } catch (Exception e) {
                        throw new BusinessException("Exception when trying to select the tenant by id", e);
                    }
                }
            }
        } else {
            try {
                client.selectTenant(tenantName);
                return;
            } catch (Exception e) {
                throw new BusinessException("Exception when trying to select the tenant by name", e);
            }
        }
        throw new BusinessException("No tenant was found for " +
                (tenantName.isEmpty() ? "project id '" + projectId + "'" : "project name '" + tenantName + "'"));
    }

    private StepClient getClient() {
        StepClient client = getSession().get(StepClient.class);
        if (client == null) {
            throw new BusinessException("The Step Client should be initialized with the 'InitStepClient' keyword");
        }
        return client;
    }

    @Keyword(schema = "{\"properties\":{},\"required\":[]}",
            properties = {""},
            description = "Keyword used to list the existing tenants/projects.")
    public void ListTenants() throws BusinessException {

        StepClient client = getClient();

        try {
            if (client.getCurrentTenant() == null) {
                throw new BusinessException("client.getCurrentTenant() is null");
            }

            output.add("CurrentTenant", client.getCurrentTenant().getName());
            String id = client.getCurrentTenant().getProjectId();
            if (id != null) {
                output.add("CurrentProjectId", id);
            }
            List<String> resultName = new ArrayList<>();
            List<String> resultId = new ArrayList<>();
            client.getAvailableTenants().forEach(tenant -> {
                resultName.add(tenant.getName());
                resultId.add(tenant.getProjectId());
            });
            String oldJson = "{\"name\":[\"" + "".join("\",\"", resultName) + "\"],\"id\":[\"" + "".join("\",\"", resultId) + "\"]}";
            output.add("Tenants", oldJson);
        } catch (Exception e) {
            throw new BusinessException("Exception when trying to list the tenants", e);
        }
    }

    @Keyword(schema = "{\"properties\":{"
            + "\"File\":{\"type\":\"string\"},"
            + "\"Type\":{\"type\":\"string\"}"
            + "},\"required\":[\"File\"]}",
            properties = {""},
            description = "Keyword used to upload a file as a resource.")
    public void UploadResource() throws BusinessException {

        StepClient client = getClient();

        String fileName = input.getString("File");
        String type = input.getString("Type", ResourceManager.RESOURCE_TYPE_TEMP);
        File file = new File(fileName);

        if (!file.exists()) {
            output.setBusinessError("\"" + fileName + "\" does not exist.");
            return;
        }
        if (!file.isFile()) {
            output.setBusinessError("\"" + fileName + "\" is not a file.");
            return;
        }

        try (FileInputStream stream = new FileInputStream(file)) {
            Resource resource = client.getResourceManager().createResource(type, stream, file.getName(), false, null);
            output.add("ResourceId", resource.getId().toString());
        } catch (IOException e) {
            throw new BusinessException("IOException when trying to upload the file '" + fileName + "'", e);
        } catch (SimilarResourceExistingException e) {
            throw new BusinessException("SimilarResourceExistingException when trying to upload the file '" + fileName + "'", e);
        }
    }

    @Keyword(schema = "{\"properties\":{"
            + "\"ResourceID\":{\"type\":\"string\"},"
            + "\"Destination\":{\"type\":\"string\"},"
            + "\"DeleteAfter\":{\"type\":\"boolean\"}"
            + "},\"required\":[\"ResourceID\",\"Destination\"]}",
            properties = {""},
            description = "Keyword used to download a resource into a file.")
    public void DownloadResource() throws BusinessException {

        StepClient client = getClient();

        String resourceID = input.getString("ResourceID");
        String destination = input.getString("Destination");
        boolean deleteAfter = input.getBoolean("DeleteAfter", false);

        try (Downloader downloader = new Downloader()) {

            File file = new File(destination);
            File resourceFile;
            if (!file.isDirectory()) {
                resourceFile = file;
            } else {
                String fileName = "tmp"; //resource.getResourceName();
                resourceFile = new File(destination + File.separatorChar + fileName);
            }
            output.add("File", resourceFile.getAbsolutePath());
            if (!resourceFile.exists() && !resourceFile.createNewFile()) {
                throw new BusinessException("Could not create destination file \"" + resourceFile.getAbsolutePath() + "\".");
            }
            try (InputStream inputStream = downloader.getResourceContent(resourceID, destination);
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
                } catch (ControllerClientException ignored) {
                }
            }
        } catch (IOException e) {
            throw new BusinessException("IOException when trying to download the resource '" + resourceID + "'", e);
        }
    }

    static class Downloader extends AbstractRemoteClient {
        private InputStream getResourceContent(String resourceId, String destination) throws IOException {
            final Invocation.Builder b = requestBuilder("/rest/resources/" + resourceId + "/content");
            return (InputStream) b.get().getEntity();
        }
    }

    @Keyword(schema = "{\"properties\":{"
            + "\"PlanName\":{\"type\":\"string\"},"
            + "\"Description\":{\"type\":\"string\"},"
            + "\"CustomParameters\":{\"type\":\"string\"},"
            + "\"Timeout\":{\"type\":\"string\"},"
            + "\"Async\":{\"type\":\"boolean\"},"
            + "\"UserId\":{\"type\":\"string\"}"
            + "},\"required\":[\"PlanName\"]}",
            properties = {""},
            timeout = 1800000,
            description = "Keyword used to execute a plan given its name.")
    public void RunLocalExecution() throws BusinessException, IOException, InterruptedException {
        String planName = getMandatoryInputString("PlanName");
        ;
        String description = input.getString("Description", planName);
        String customParametersJson = input.getString("CustomParameters", "{}");
        String userId = input.getString("UserId", "");
        boolean async = input.getBoolean("Async", false);
        long timeout = Long.parseLong(input.getString("Timeout", DEFAULT_TIMEOUT));

        String planId = findPlanId(planName);

        runExecution("local", "{\"planid\":\"" + planId + "\"}", description, customParametersJson, userId, async, timeout);
    }

    @Keyword(schema = "{\"properties\":{"
            + "\"RepositoryID\":{\"type\":\"string\"},"
            + "\"RepositoryParameters\":{\"type\":\"string\"},"
            + "\"Description\":{\"type\":\"string\"},"
            + "\"CustomParameters\":{\"type\":\"string\"},"
            + "\"Timeout\":{\"type\":\"string\"},"
            + "\"Async\":{\"type\":\"boolean\"},"
            + "\"UserId\":{\"type\":\"string\"}"
            + "},\"required\":[\"RepositoryID\",\"RepositoryParameters\",\"Description\",\"CustomParameters\"]}",
            properties = {""},
            timeout = 1800000,
            description = "Keyword used to execute a plan given a repository.")
    public void RunExecution() throws BusinessException, IOException, InterruptedException {
        String repoId = getMandatoryInputString("RepositoryID");
        String repoParametersJson = getMandatoryInputString("RepositoryParameters");
        String description = getMandatoryInputString("Description");
        String customParametersJson = getMandatoryInputString("CustomParameters");
        String userId = input.getString("UserId", "");
        boolean async = input.getBoolean("Async", false);

        long timeout = Long.parseLong(input.getString("Timeout", DEFAULT_TIMEOUT));

        runExecution(repoId, repoParametersJson, description, customParametersJson, userId, async, timeout);
    }

    @Keyword(schema = "{\"properties\":{"
            + "\"Id\":{\"type\":\"string\"},"
            + "\"Wait\":{\"type\":\"boolean\"},"
            + "\"WaitTimeout\":{\"type\":\"string\"}"
            + "},\"required\":[\"Id\"]}",
            properties = {""},
            description = "Keyword used to stop an execution.")
    public void StopExecution() throws BusinessException {

        String execId = getMandatoryInputString("Id");
        boolean wait = input.getBoolean("Wait", false);
        long timeout = Long.parseLong(input.getString("Timeout", DEFAULT_TIMEOUT));

        StepClient client = getClient();

        Execution exec = client.getExecutionManager().get(execId);
        if (exec == null) {
            throw new BusinessException("No execution with Id '" + execId + "' found");
        } else {
            if (exec.getStatus() != ExecutionStatus.ENDED) {
                client.getExecutionManager().stop(execId);
                if (wait) {
                    try {
                        client.getExecutionManager().waitForTermination(execId, timeout);
                    } catch (TimeoutException e) {
                        throw new BusinessException("Timeout waiting for termination of execution Id '" + execId + "'");
                    } catch (InterruptedException ignored) {
                    }
                }
            } else {
                throw new BusinessException("Execution Id '" + execId + "' is not running. Status is '"+exec.getStatus()+"'");
            }
        }
    }

    @Keyword(schema = "{\"properties\":{},\"required\":[]}",
            properties = {""},
            description = "Keyword used to search for an execution. Any input will be interpreted as a criteria to search")
    public void FindExecution() throws BusinessException {
        StepClient client = getClient();

        Map<String, String> searchParam = new HashMap<>();
        input.forEach(
                (k, v) -> searchParam.put(k, String.valueOf(v))
        );

        StringBuilder executions = new StringBuilder("[");
        client.getRemoteAccessors().getAbstractAccessor("executions", Execution.class)
                .findManyByCriteria(searchParam)
                .forEach(e -> {
                    executions.append(String.format("{'id':'%s','status':'%s'}", e.getId(), e.getStatus()));
                });
        executions.append("]");
        output.add("Executions", executions.toString());
    }

    protected String findPlanId(String planName) {
        StepClient client = getClient();

        Map<String, String> attributes = new HashMap<>();
        attributes.put(AbstractOrganizableObject.NAME, planName);

        Plan plan = client.getRemoteAccessors().getAbstractAccessor("plans", Plan.class).findByAttributes(attributes);

        if (plan == null) {
            throw new BusinessException("Could not find plan named '" + planName + "'");
        }

        return plan.getId().toString();
    }

    protected void runExecution(String repoId, String repoParametersJson, String description, String customParametersJson,
                                String userId, boolean async, long timeout) throws BusinessException, IOException, InterruptedException {
        StepClient client = getClient();

        ExecutionParameters executionParams = new ExecutionParameters();

        executionParams.setMode(ExecutionMode.RUN);
        if (userId.isEmpty()) {
            executionParams.setUserID((String) getSession().get("User"));
        } else {
            executionParams.setUserID(userId);
        }
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

        if (!async) {

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
    }

    private String getMandatoryInputString(String inputName) throws BusinessException {
        if (!input.containsKey(inputName)) {
            throw new BusinessException("The mandatory input parameter '" + inputName + "' is missing");
        } else {
            return input.getString(inputName);
        }
    }
}