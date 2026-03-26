package votingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RegisterWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vtw_register";
    }

    @Override
    public TaskResult execute(Task task) {
        String voterId = (String) task.getInputData().get("voterId");
        String precinct = (String) task.getInputData().get("precinct");
        System.out.printf("  [register] Voter %s registered at precinct %s%n", voterId, precinct);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("registered", true);
        return result;
    }
}
