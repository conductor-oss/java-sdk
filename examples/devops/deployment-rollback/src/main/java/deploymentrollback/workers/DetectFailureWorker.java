package deploymentrollback.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectFailureWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rb_detect_failure";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [detect] checkout-service failure: error-rate-spike");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("detect_failureId", "DETECT_FAILURE-1335");
        result.addOutputData("success", true);
        return result;
    }
}
