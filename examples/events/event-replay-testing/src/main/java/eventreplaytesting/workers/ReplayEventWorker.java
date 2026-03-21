package eventreplaytesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Replays a single recorded event in the sandbox environment.
 * Input: events (list), sandboxId, iteration (0-based loop index)
 * Output: result, expectedResult, eventId
 */
public class ReplayEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rt_replay_event";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events = (List<Map<String, Object>>) task.getInputData().get("events");
        String sandboxId = (String) task.getInputData().get("sandboxId");
        if (sandboxId == null) {
            sandboxId = "unknown";
        }

        int iteration = 0;
        Object iterObj = task.getInputData().get("iteration");
        if (iterObj instanceof Number) {
            iteration = ((Number) iterObj).intValue();
        }

        String eventId = "unknown";
        String expectedResult = "unknown";

        if (events != null && iteration < events.size()) {
            Map<String, Object> event = events.get(iteration);
            eventId = (String) event.getOrDefault("id", "unknown");
            expectedResult = (String) event.getOrDefault("expected", "unknown");
        }

        System.out.println("  [rt_replay_event] Replaying event " + eventId + " in sandbox " + sandboxId
                + " (iteration " + iteration + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "processed");
        result.getOutputData().put("expectedResult", expectedResult);
        result.getOutputData().put("eventId", eventId);
        return result;
    }
}
