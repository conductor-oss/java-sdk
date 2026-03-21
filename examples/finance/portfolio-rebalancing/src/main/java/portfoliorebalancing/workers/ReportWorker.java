package portfoliorebalancing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "prt_report"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Rebalancing report — " + task.getInputData().get("tradesExecuted") + " trades, verified: " + task.getInputData().get("verified"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportId", "RPT-REBAL-2024-001");
        result.getOutputData().put("generatedAt", Instant.now().toString());
        return result;
    }
}
