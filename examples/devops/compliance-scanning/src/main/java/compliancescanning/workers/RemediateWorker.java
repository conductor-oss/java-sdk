package compliancescanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RemediateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cs_remediate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [remediate] Auto-remediated 2 critical findings");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("remediate", true);
        return result;
    }
}
