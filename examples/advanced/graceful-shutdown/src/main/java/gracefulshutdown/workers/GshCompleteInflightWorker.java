package gracefulshutdown.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GshCompleteInflightWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gsh_complete_inflight";
    }

    @Override
    public TaskResult execute(Task task) {
        Object inf = task.getInputData().get("inFlightTasks");
        int count = inf instanceof java.util.List ? ((java.util.List<?>) inf).size() : 0;
        System.out.println("  [complete] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("completedCount", count);
        result.getOutputData().put("completedTasks", inf);
        return result;
    }
}