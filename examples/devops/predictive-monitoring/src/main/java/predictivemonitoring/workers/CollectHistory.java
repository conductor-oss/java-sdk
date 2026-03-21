package predictivemonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectHistory implements Worker {

    @Override public String getTaskDefName() { return "pdm_collect_history"; }

    @Override
    public TaskResult execute(Task task) {
        String metricName = (String) task.getInputData().get("metricName");
        Object historyDays = task.getInputData().get("historyDays");
        System.out.println("[pdm_collect_history] Collecting " + historyDays + " days of " + metricName + " history...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dataPoints", 43200);
        result.getOutputData().put("metricName", metricName);
        result.getOutputData().put("granularity", "1m");
        result.getOutputData().put("oldest", "2026-02-06T00:00:00Z");
        result.getOutputData().put("newest", "2026-03-08T00:00:00Z");
        return result;
    }
}
