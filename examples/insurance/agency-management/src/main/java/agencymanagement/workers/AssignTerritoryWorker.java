package agencymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssignTerritoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agm_assign_territory";
    }

    @Override
    public TaskResult execute(Task task) {

        String agentId = (String) task.getInputData().get("agentId");
        System.out.printf("  [territory] Agent %s assigned territory%n", agentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("territory", "CA-West");
        result.getOutputData().put("zipCodes", 45);
        return result;
    }
}
