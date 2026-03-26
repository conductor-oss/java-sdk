package citizenrequest.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ClassifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ctz_classify";
    }

    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().get("requestId");
        System.out.printf("  [classify] Request %s classified as infrastructure%n", requestId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("category", "infrastructure");
        result.getOutputData().put("priority", "medium");
        return result;
    }
}
