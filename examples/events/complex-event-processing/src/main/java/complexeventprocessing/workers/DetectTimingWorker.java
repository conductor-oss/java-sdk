package complexeventprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Checks if any gap between consecutive event timestamps exceeds maxGapMs.
 * Input: events (list of event maps with "ts" key), maxGapMs (number)
 * Output: violation (true if any gap exceeds maxGapMs), overallResult ("pattern_found" or "normal")
 */
public class DetectTimingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cp_detect_timing";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) task.getInputData().get("events");
        if (events == null) {
            events = List.of();
        }

        Object maxGapObj = task.getInputData().get("maxGapMs");
        long maxGapMs = 5000;
        if (maxGapObj instanceof Number) {
            maxGapMs = ((Number) maxGapObj).longValue();
        }

        boolean violation = false;
        for (int i = 1; i < events.size(); i++) {
            long prevTs = ((Number) events.get(i - 1).get("ts")).longValue();
            long currTs = ((Number) events.get(i).get("ts")).longValue();
            long gap = currTs - prevTs;
            if (gap > maxGapMs) {
                violation = true;
                break;
            }
        }

        String overallResult = violation ? "pattern_found" : "normal";

        System.out.println("  [cp_detect_timing] Max gap check: " + (violation ? "VIOLATION found" : "within limits") + " -> " + overallResult);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("violation", violation);
        result.getOutputData().put("overallResult", overallResult);
        return result;
    }
}
