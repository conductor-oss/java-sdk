package accessreview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RequestCertificationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ar_request_certification";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [certify] Manager approved 5 revocations, kept 6");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("request_certification", true);
        result.addOutputData("processed", true);
        return result;
    }
}
