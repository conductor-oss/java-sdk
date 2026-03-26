package custommetrics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Updates the dashboard with aggregated metrics.
 */
public class UpdateDashboard implements Worker {

    @Override
    public String getTaskDefName() {
        return "cus_update_dashboard";
    }

    @Override
    public TaskResult execute(Task task) {
        Object metricCount = task.getInputData().get("metricCount");

        System.out.println("[cus_update_dashboard] Updating dashboard with " + metricCount + " aggregated metrics...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dashboardUpdated", true);
        result.getOutputData().put("dashboardUrl", "https://dashboard.example.com/custom-metrics");
        result.getOutputData().put("updatedAt", "2026-03-08T06:01:00Z");
        return result;
    }
}
