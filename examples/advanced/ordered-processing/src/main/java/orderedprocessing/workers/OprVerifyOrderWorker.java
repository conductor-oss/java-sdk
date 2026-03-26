package orderedprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OprVerifyOrderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "opr_verify_order";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [verify] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("orderCorrect", true);
        result.getOutputData().put("processedSequence", task.getInputData().getOrDefault("processedOrder", java.util.List.of()));
        return result;
    }
}