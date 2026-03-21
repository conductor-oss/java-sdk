package citizenrequest.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ResolveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ctz_resolve";
    }

    @Override
    public TaskResult execute(Task task) {
        String department = (String) task.getInputData().get("department");
        System.out.printf("  [resolve] %s resolved the request%n", department);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("resolution", "Pothole repaired at Main St");
        return result;
    }
}
