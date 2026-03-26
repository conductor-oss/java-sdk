package orderedprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class OprReceiveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "opr_receive";
    }

    @Override
    public TaskResult execute(Task task) {
        Object msgs = task.getInputData().getOrDefault("messages", java.util.List.of());
        System.out.println("  [receive] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("receivedMessages", msgs);
        result.getOutputData().put("count", msgs instanceof java.util.List ? ((java.util.List<?>) msgs).size() : 0);
        return result;
    }
}