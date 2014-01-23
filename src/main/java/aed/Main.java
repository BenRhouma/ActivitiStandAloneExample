package aed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

public class Main {

    public static void main(String[] args) {
        ProcessEngine processEngine = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration()
                .setDatabaseSchemaUpdate(
                        ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                .setJdbcUrl("jdbc:h2:mem:my-own-db;DB_CLOSE_DELAY=1000")
                .setJobExecutorActivate(true)
                .setDatabaseSchemaUpdate("drop-create").buildProcessEngine();

        try {

            RepositoryService repositoryService = processEngine
                    .getRepositoryService();
            RuntimeService runtimeService = processEngine.getRuntimeService();

            Deployment deployment = repositoryService.createDeployment()
                    .addClasspathResource("diagrams/helloworld.bpmn").deploy();

            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("employeeName", "Kermit");
            variables.put("numberOfDays", new Integer(4));
            variables.put("vacationMotivation", "I'm really tired!");

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("vacationRequest", variables);

            System.out.println("Started Process instance id "
                    + processInstance.getProcessInstanceId());

            TaskService taskService = processEngine.getTaskService();
            List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup("management").list();
            for (Task task : tasks) {
                System.out.println(task.getName());
            }
        } catch (Exception e) {
        } finally {
            processEngine.close();
        }

    }
}
