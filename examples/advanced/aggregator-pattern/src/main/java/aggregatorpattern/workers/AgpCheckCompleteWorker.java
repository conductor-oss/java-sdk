package aggregatorpattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AgpCheckCompleteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agp_check_complete";
    }

    @Override
    public TaskResult execute(Task task) {
        int collected = task.getInputData().get("collectedCount") instanceof Number ? ((Number) task.getInputData().get("collectedCount")).intValue() : 0;
        int expected = task.getInputData().get("expectedCount") instanceof Number ? ((Number) task.getInputData().get("expectedCount")).intValue() : 0;
        boolean isComplete = collected >= expected;
        System.out.println("  [check] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("isComplete", isComplete);
        result.getOutputData().put("collectedCount", collected);
        result.getOutputData().put("expectedCount", expected);
        return result;
    }
}