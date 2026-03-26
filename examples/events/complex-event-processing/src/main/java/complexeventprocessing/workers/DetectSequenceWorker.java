package complexeventprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Detects whether "login" appears before "purchase" in the event sequence.
 * Input: events (list of event maps with "type" key), rule (string)
 * Output: detected (true if login before purchase), rule
 */
public class DetectSequenceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cp_detect_sequence";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) task.getInputData().get("events");
        if (events == null) {
            events = List.of();
        }

        String rule = (String) task.getInputData().get("rule");
        if (rule == null) {
            rule = "unknown";
        }

        int loginIdx = -1;
        int purchaseIdx = -1;
        for (int i = 0; i < events.size(); i++) {
            String type = (String) events.get(i).get("type");
            if ("login".equals(type) && loginIdx < 0) {
                loginIdx = i;
            }
            if ("purchase".equals(type) && purchaseIdx < 0) {
                purchaseIdx = i;
            }
        }

        boolean detected = loginIdx >= 0 && purchaseIdx > loginIdx;

        System.out.println("  [cp_detect_sequence] Rule \"" + rule + "\": " + (detected ? "DETECTED" : "not found"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("detected", detected);
        result.getOutputData().put("rule", rule);
        return result;
    }
}
