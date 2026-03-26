package troubleticket.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ResolveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tbt_resolve";
    }

    @Override
    public TaskResult execute(Task task) {

        String assignee = (String) task.getInputData().get("assignee");
        System.out.println("  [resolve] Cable replaced");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("resolution", "cable-replaced");
        result.getOutputData().put("downtime", "4 hours");
        return result;
    }
}
