package intrusiondetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessSeverityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "id_assess_severity";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [severity] Critical threat: active brute-force from known malicious IP");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("assess_severity", true);
        result.addOutputData("processed", true);
        return result;
    }
}
