package intrusiondetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CorrelateThreatsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "id_correlate_threats";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [correlate] IP matched known threat intelligence feed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("correlate_threats", true);
        result.addOutputData("processed", true);
        return result;
    }
}
