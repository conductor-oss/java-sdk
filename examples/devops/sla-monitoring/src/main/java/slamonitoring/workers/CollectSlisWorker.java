package slamonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectSlisWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sla_collect_slis";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sli] payment-api: 99.95% availability, p99 = 180ms");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("collect_slisId", "COLLECT_SLIS-1339");
        result.addOutputData("success", true);
        return result;
    }
}
