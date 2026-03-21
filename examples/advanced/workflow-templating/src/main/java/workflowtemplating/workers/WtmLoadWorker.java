package workflowtemplating.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WtmLoadWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wtm_load";
    }

    @Override
    public TaskResult execute(Task task) {
        String destination = (String) task.getInputData().getOrDefault("destination", "unknown");
        System.out.println("  [load] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("loadedCount", 15000);
        result.getOutputData().put("destination", destination);
        return result;
    }
}