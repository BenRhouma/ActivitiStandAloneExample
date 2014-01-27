package com.sofrecom;

import static com.sofrecom.WorkflowService.processEngine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

public class Main {

    final static org.slf4j.Logger logger = LoggerFactory.getLogger(Main.class);

    public static void mainx(String[] args) {

        ProcessEngine processEngine = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                //.setJdbcUrl("jdbc:mysql://localhost:3306/workflow").setJdbcUsername("root").setJdbcDriver("com.mysql.jdbc.Driver").setJdbcPassword("root")
                .setJobExecutorActivate(true)
                .setDatabaseSchemaUpdate("drop-create")
                .buildProcessEngine();

        RepositoryService repositoryService = processEngine
                .getRepositoryService();
        repositoryService.createDeployment()
                .addClasspathResource("diagrams/reports.bpmn").deploy();
        Map<String, Object> variables = new HashMap<String, Object>();

//      variables.put("vacationMotivation", "I'm really tired!");
        RuntimeService runtimeService = processEngine.getRuntimeService();
        variables.put("choiceDCGP", true);
        runtimeService.startProcessInstanceByKey("validationProcess", variables);

        try {
            TaskService taskService = processEngine.getTaskService();
            List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("DCGP").list();
          
            Map<String, Object> taskParams = new HashMap<String, Object>();
            if (tasks.size() > 0) {
                 logger.info("excution task " +tasks.get(0).getName() );
                taskService.claim(tasks.get(0).getId(), "zied");
                taskService.complete(tasks.get(0).getId(), taskParams);
               logger.info("excution task " +tasks.get(0).getName() +" terminated");
            }
            
        } catch (Exception e) {
            logger.info(e.getMessage());
        } finally {

        }
        processEngine.close();
    }
}
