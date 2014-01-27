/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        public static String GenerateReports = "managment";
        public static String ValdationDFI = "DFI";
        public static String ValidationDCGP = "DCGP";
        public static String ValidationFinal = "DG";
    }

    public static class ProcessTaskMapping {
        public static String generateReport = "generateReport";
        public static String validatinDCGP = "validatinDCGP";
        public static String validatinDFI = "validatinDFI";
        public static String validatinFinal = "validatinFinal";
    }

    public static class ProcessVars {
        public static String DCGP_CHOICE = "validationDCGPApproved";
        public static String DFI_CHOICE = "validationDFIApproved";
        public static String DG_CHOICE = "validationDGApproved";
    }

    static ProcessEngine processEngine;

    private static void initProcessEngine() {
        processEngine = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                //.setJdbcUrl("jdbc:mysql://localhost:3306/workflow").setJdbcUsername("root").setJdbcDriver("com.mysql.jdbc.Driver").setJdbcPassword("root")
                .setJobExecutorActivate(true)
                .setDatabaseSchemaUpdate("drop-create")
                .buildProcessEngine();
    }

    public static void initWorkFlow() {
        initProcessEngine();
        RepositoryService repositoryService = processEngine
                .getRepositoryService();
        repositoryService.createDeployment()
                .addClasspathResource("diagrams/validationProcess.bpmn").deploy();
    }

    public static void startProcess(String processId) {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("validationDCGPApproved", false);
        variables.put("validationDFIApproved", false);
        variables.put("validationDG", false);
        variables.put("reportId", processId);
//      variables.put("vacationMotivation", "I'm really tired!");
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstanceQuery process = runtimeService.createProcessInstanceQuery().variableValueEquals("reportId", processId);

        if (process.count() == 0) {
            logger.info("creating new process instance:");
            runtimeService.startProcessInstanceByKey("validationProcess", variables);
        }

    }

    public static void refresh() {

    }

    public static void stop() {
        processEngine.close();
    }

    public static void generateReportss(String one) {
        try {
            TaskService taskService = processEngine.getTaskService();
            List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(ProcessGroupsMapping.GenerateReports).processVariableValueEquals("reportId", one).list();

            if (tasks.size() > 0) {
                taskService.claim(tasks.get(0).getId(), "zied");
                taskService.complete(tasks.get(0).getId());
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
        } finally {

        }
    }

    static void terminate(String id) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstanceQuery process = runtimeService.createProcessInstanceQuery().variableValueEquals("reportId", id);
        if (process.count() > 0) {
            runtimeService.deleteProcessInstance(process.list().get(0).getId(), "nothing");
        }

    }

    static void validateTask(String taskId, String group, String reportId, Boolean choice, String globaVariable, String localVariable) {
        logger.info("validation " + group);
        try {
            TaskService taskService = processEngine.getTaskService();
            List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(group).taskDefinitionKey(taskId).processVariableValueEquals("reportId", reportId).list();
            for (Task task : tasks) {
                System.out.println(task.getName());
            }
            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put(localVariable, choice);
            if (tasks.size() > 0) {
                taskService.claim(tasks.get(0).getId(), "zied");
                taskService.complete(tasks.get(0).getId(), variables);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
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
            TaskService taskService = processEngine.getTaskService();
            List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskId).taskCandidateGroup(group).processVariableValueEquals("reportId", reportVariable).list();
            for (Task task : tasks) {
                System.out.println(task.getName());
            }

            if (tasks.size() > 0) {
                return true;
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
        } finally {

        }
        return false;
    }

}
