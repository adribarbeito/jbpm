package eu.barbeito.jbpm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jbpm.api.Configuration;
import org.jbpm.api.Execution;
import org.jbpm.api.ExecutionService;
import org.jbpm.api.ProcessDefinition;
import org.jbpm.api.ProcessInstance;
import org.jbpm.api.ProcessInstanceQuery;
import org.jbpm.api.RepositoryService;
import org.jbpm.api.TaskQuery;
import org.jbpm.api.TaskService;
import org.jbpm.api.task.Task;

/**
 * Utility class for doing deletes on a jBPM Workflow.
 * 
 * @author Adrian Barbeito
 *
 */
public class JbpmUtils {
	
	private static final JbpmUtils instance = new JbpmUtils();
	 
	private static final ExecutionService executionService = Configuration.getProcessEngine().getExecutionService();
	private static final RepositoryService repositoryService = Configuration.getProcessEngine().getRepositoryService();
	private static final TaskService taskService = Configuration.getProcessEngine().getTaskService();
	
	private JbpmUtils() {}

	public static JbpmUtils getInstance() {
		return instance;
	}

	/**
     * Delete all the process instances related to a processKey.
	 * 
	 * @param processKey
	 */
	public void deleteProcessInstances(final String processKey) {

		final List<String> processDefinitionsList = getProcessDefinitionIds(processKey);

		for (final String processDefinitionId : processDefinitionsList) {
			final ProcessInstanceQuery query = executionService.createProcessInstanceQuery();
			query.processDefinitionId(processDefinitionId);
			final List<ProcessInstance> processInstanceList = query.list();

			for (final ProcessInstance processInstance : processInstanceList) {
				deleteProcessInstance(processInstance.getId());
			}
		}
	}
	
	/**
	 * Delete all the process instance related to a process definition with tasks created before the number of days 
	 * indicated. Days can be null.
	 * 
	 * @param processDefinitionId
	 * @param days
	 */
	public void deleteProcessInstances(final String processDefinitionId, final Integer days) {
		final Date limitDate = getLimitDate(days);
		
		final List<Task> tasks = getTasks(processDefinitionId);
		for (final Task task : tasks) {
			
			if (isPreviousTask(task, limitDate)) {
				deleteProcessInstance(getProcessInstanceId(task));
			}
			
		}
	}
	
	/**
	 * Delete the process instance with the specified identifier.
	 * 
	 * @param id
	 */
	public void deleteProcessInstance(final String id) {
		System.out.println("Deleting " + id);
		try {
			executionService.deleteProcessInstance(id);
		} catch (Exception exc) {
			System.out.println("ERROR deleting " + id);
		}
	}
	
	/**
	 * Get all the process definition id related to the processKey.
	 * 
	 * @param processKey
	 * @return
	 */
	public List<String> getProcessDefinitionIds(final String processKey) {
		final List<ProcessDefinition> processDefinitionList = 
				repositoryService.createProcessDefinitionQuery().processDefinitionKey(processKey).list();
		
		final List<String> processDefinitionIds = new ArrayList<String>();
		for (final ProcessDefinition processDefinition : processDefinitionList) {
			processDefinitionIds.add(processDefinition.getId());
		}
		return processDefinitionIds;
	}
	
	/**
	 * Get the processInstanceId associated to the indicated task.
	 * 
	 * @param task
	 * @return
	 */
	public String getProcessInstanceId(final Task task) {
		final Execution execution = executionService.findExecutionById(task.getExecutionId());
		if (execution.getProcessInstance() == null) {
			return null;
		} else {
			return execution.getProcessInstance().getId();
		}
	}
	
	/**
	 * Get all the tasks related the processDefinitionId.
	 * 
	 * @param processDefinitionId
	 * @return
	 */
	public List<Task> getTasks(String processDefinitionId) {
		final TaskQuery taskQuery = taskService.createTaskQuery();
		taskQuery.processDefinitionId(processDefinitionId);
		return taskQuery.list();
	}
	
	/**
	 * Deploy a workflow.
	 * 
	 * @param pathResource
	 */
	public void deployWorkflow(final String pathResource) {
		repositoryService.createDeployment().addResourceFromClasspath(pathResource).deploy();
	}
	
	/**
	 * WARNING! remove a workflow from the storage system and ALL the related information stored (processes, tasks,
	 * variables, history...).
	 * 
	 * @param workflowId
	 */
	public void dropWorkflow(final String workflowId) {
		repositoryService.deleteDeploymentCascade(workflowId);
	}
	
	/**
	 * Check if the task was created before the limit date.
	 * 
	 * @param task
	 * @param limitDate
	 * @return
	 */
	private boolean isPreviousTask(final Task task, final Date limitDate) {
		return limitDate == null ? true : task.getCreateTime().getTime() < limitDate.getTime();
	}

	/**
	 * Get the limit date deducting days to now.
	 * 
	 * @param days
	 * @return
	 */
	private Date getLimitDate(final int days) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -days);
		return calendar.getTime();
	}
}

