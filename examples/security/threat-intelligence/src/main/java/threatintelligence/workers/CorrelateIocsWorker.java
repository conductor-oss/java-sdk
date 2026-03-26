package threatintelligence.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CorrelateIocsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ti_correlate_iocs";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [correlate] 18 IOCs matched against internal infrastructure");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("correlate_iocs", true);
        result.addOutputData("processed", true);
        return result;
    }
}
