package qualityinspection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecordWorker implements Worker {
    @Override public String getTaskDefName() { return "qi_record"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [record] QI result for " + task.getInputData().get("product") +
                " batch " + task.getInputData().get("batchId") + ": " + task.getInputData().get("result"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recorded", true);
        return result;
    }
}
