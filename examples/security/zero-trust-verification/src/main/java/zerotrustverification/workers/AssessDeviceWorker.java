package zerotrustverification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessDeviceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "zt_assess_device";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [device] LAPTOP-A1B2C3: compliant, patched, encrypted");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("assess_device", true);
        result.addOutputData("processed", true);
        return result;
    }
}
