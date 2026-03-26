package eventreplay.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Generates a summary report from the replay results.
 * Returns reportId, success rate, success/fail counts, and a fixed generatedAt timestamp.
 */
public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ep_generate_report";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> replayResults =
                (List<Map<String, Object>>) task.getInputData().get("replayResults");
        if (replayResults == null) {
            replayResults = List.of();
        }

        Object totalReplayedObj = task.getInputData().get("totalReplayed");
        int totalReplayed = 0;
        if (totalReplayedObj instanceof Number) {
            totalReplayed = ((Number) totalReplayedObj).intValue();
        }

        String sourceStream = (String) task.getInputData().get("sourceStream");
        if (sourceStream == null || sourceStream.isBlank()) {
            sourceStream = "unknown-stream";
        }

        System.out.println("  [ep_generate_report] Generating report for " + sourceStream
                + " with " + totalReplayed + " replayed events");

        int successCount = 0;
        int failCount = 0;
        for (Map<String, Object> entry : replayResults) {
            String replayStatus = (String) entry.get("replayStatus");
            if ("success".equals(replayStatus)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        int successRate = totalReplayed > 0 ? (successCount * 100) / totalReplayed : 0;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportId", "rpt-fixed-001");
        result.getOutputData().put("successRate", successRate);
        result.getOutputData().put("successCount", successCount);
        result.getOutputData().put("failCount", failCount);
        result.getOutputData().put("generatedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
