package usageanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "uag_process";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [process] Records processed — deduplicated and normalized");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedRecords", 1150000);
        result.getOutputData().put("duplicatesRemoved", 50000);
        return result;
    }
}
