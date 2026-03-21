package workflowtemplating.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WtmExtractWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wtm_extract";
    }

    @Override
    public TaskResult execute(Task task) {
        String source = (String) task.getInputData().getOrDefault("source", "unknown");
        System.out.println("  [extract] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", "extracted_data");
        result.getOutputData().put("recordCount", 15000);
        return result;
    }
}