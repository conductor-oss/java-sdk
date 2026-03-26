package exceptionhandling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for eh_analyze — evaluates risk score and routes accordingly.
 *
 * If risk > 7, sets route="human_review" (requires human intervention).
 * Otherwise, sets route="auto_process" (automated processing).
 */
public class AnalyzeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eh_analyze";
    }

    @Override
    public TaskResult execute(Task task) {
        int risk = 0;
        Object riskInput = task.getInputData().get("risk");
        if (riskInput instanceof Number) {
            risk = ((Number) riskInput).intValue();
        }

        String route = risk > 7 ? "human_review" : "auto_process";

        System.out.println("  [eh_analyze] risk=" + risk + " -> route=" + route);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("route", route);
        result.getOutputData().put("risk", risk);
        return result;
    }
}
