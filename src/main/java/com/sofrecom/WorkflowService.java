/*
 * Copyright 2014 z.benrhouma.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sofrecom;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.slf4j.LoggerFactory;

/**
 *
 * @author z.benrhouma
 */
public class WorkflowService {

    public final static org.slf4j.Logger logger = LoggerFactory.getLogger(WorkflowService.class);

    public static class ProcessGroupsMapping {

        public static String GENERATE_REPORTS = "managment";
        public static String DFI_VALIDATION = "DFI";
        public static String DCGP_VALIDATION = "DCGP";
        public static String FINAL_VALDIATION = "DG";
    }

    public static class ProcessTaskMapping {

        public static String GENERATE_REPORTS = "generateReport";
        public static String DCGP_VALIDATION = "validatinDCGP";
        public static String DFI_VALIDATION = "validatinDFI";
        public static String FINAL_VALDIATION = "validatinFinal";
    }

    public static class ProcessVars {

        public static String DCGP_CHOICE = "validationDCGPApproved";
        public static String DFI_CHOICE = "validationDFIApproved";
        public static String DG_CHOICE = "validationDGApproved";
    }

    static ProcessEngine processEngine;

    private static Boolean initProcessEngineIsDone() {
        if(processEngine == null){
            processEngine = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                //.setJdbcUrl("jdbc:mysql://localhost:3306/workflow").setJdbcUsername("root").setJdbcDriver("com.mysql.jdbc.Driver").setJdbcPassword("root")
                .setJobExecutorActivate(true)
                .setDatabaseSchemaUpdate("drop-create")
                .buildProcessEngine();
            return true;
        }
        return false;
    }

    /**
     * buid workflow engine and deploy the validation process
     */
    public static void initWorkFlow() {
        if(initProcessEngineIsDone())
        {
            final RepositoryService repositoryService = processEngine
                    .getRepositoryService();
            repositoryService.createDeployment()
                    .addClasspathResource("diagrams/validationProcess.bpmn").deploy();
        }
    }

    private static Map<String, Object> buildProcessVariables(final String processId) {
        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("validationDCGPApproved", false);
        variables.put("validationDFIApproved", false);
        variables.put("validationDG", false);
        variables.put("reportId", processId);
        return variables;
    }

    public static void startProcess(String processId) {
        final RuntimeService runtimeService = processEngine.getRuntimeService();
        final ProcessInstanceQuery process = runtimeService.createProcessInstanceQuery().variableValueEquals("reportId", processId);
        if (process.count() == 0) {
            logger.info("creating new process instance:");
            runtimeService.startProcessInstanceByKey("validationProcess", buildProcessVariables(processId));
        }
    }

    /**
     * stop the process
     */
    public static void stop() {
        processEngine.close();
    }

    public static void generateReports(String one) {
        try {
            final TaskService taskService = processEngine.getTaskService();
            final List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(ProcessGroupsMapping.GENERATE_REPORTS).processVariableValueEquals("reportId", one).list();

            if (tasks.size() > 0) {
                taskService.claim(tasks.get(0).getId(), "zied");
                taskService.complete(tasks.get(0).getId());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException("workflow error during generating report task");
        } finally {

        }
    }

    static void terminate(String id) {
        final RuntimeService runtimeService = processEngine.getRuntimeService();
        final ProcessInstanceQuery process = runtimeService.createProcessInstanceQuery().variableValueEquals("reportId", id);
        if (process.count() > 0) {
            runtimeService.deleteProcessInstance(process.list().get(0).getId(), "nothing");
        }

    }

    static void validateTask(String taskId, String group, String reportId, Boolean choice, String globaVariable, String localVariable) {
        logger.info("validation " + group);
        try {
            final TaskService taskService = processEngine.getTaskService();
            final List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(group).taskDefinitionKey(taskId).processVariableValueEquals("reportId", reportId).list();
            for (Task task : tasks) {
                System.out.println(task.getName());
            }
            final Map<String, Object> variables = new HashMap<String, Object>();
            variables.put(localVariable, choice);
            if (tasks.size() > 0) {
                taskService.claim(tasks.get(0).getId(), "zied");
                taskService.complete(tasks.get(0).getId(), variables);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException("error during validation task " + taskId);
        } finally {

        }
        logger.info("terminate validation" + group);
    }

    static void validateDCGPTask(String taskId, String group, String reportId, Boolean choice) {
        validateTask(taskId, group, reportId, choice, ProcessVars.DCGP_CHOICE, "choice");
    }

    static void validateDFITask(String taskId, String group, String reportId, Boolean choice) {
        validateTask(taskId, group, reportId, choice, ProcessVars.DFI_CHOICE, "choice");
    }

    static void validateDGTask(String taskId, String group, String reportId, Boolean choice) {
        validateTask(taskId, group, reportId, choice, ProcessVars.DG_CHOICE, "choice");
    }

    static boolean isValidTaskForCurrentProcess(String taskId, String group, String reportVariable) {
        try {
            final TaskService taskService = processEngine.getTaskService();
            final List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskId).taskCandidateGroup(group).processVariableValueEquals("reportId", reportVariable).list();
            if (tasks.size() > 0) {
                return true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException("error during validation task " + taskId);
        } finally {

        }
        return false;
    }

}
