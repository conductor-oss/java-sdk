package complexeventprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Detects the absence of a "confirmation" event in the event list.
 * Input: events (list of event maps with "type" key), rule (string)
 * Output: detected (true if confirmation is missing), rule
 */
public class DetectAbsenceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cp_detect_absence";
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

        boolean hasConfirmation = false;
        for (Map<String, Object> event : events) {
            if ("confirmation".equals(event.get("type"))) {
                hasConfirmation = true;
                break;
            }
        }

        boolean detected = !hasConfirmation;

        System.out.println("  [cp_detect_absence] Rule \"" + rule + "\": confirmation " + (hasConfirmation ? "present" : "ABSENT"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("detected", detected);
        result.getOutputData().put("rule", rule);
        return result;
    }
}
