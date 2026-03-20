package dataencryption.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ClassifyDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "de_classify_data";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [classify] customer-database: PII detected — classified as sensitive");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("classify_dataId", "CLASSIFY_DATA-1355");
        result.addOutputData("success", true);
        return result;
    }
}
