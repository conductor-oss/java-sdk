package conversationalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Worker that saves the current turn to conversation history.
 * Appends user + assistant messages to the shared SESSION_STORE.
 */
public class SaveHistoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "crag_save_history";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String sessionId = (String) task.getInputData().get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }

        String userMessage = (String) task.getInputData().get("userMessage");
        String assistantMessage = (String) task.getInputData().get("assistantMessage");

        List<Map<String, String>> history = (List<Map<String, String>>) task.getInputData().get("history");
        if (history == null) {
            history = new ArrayList<>();
        }

        // Create a mutable copy and add the new turn
        history = new ArrayList<>(history);
        history.add(Map.of(
                "user", userMessage != null ? userMessage : "",
                "assistant", assistantMessage != null ? assistantMessage : ""));

        LoadHistoryWorker.SESSION_STORE.put(sessionId, history);

        System.out.println("  [save] Session \"" + sessionId + "\": now " + history.size() + " turns");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("turnNumber", history.size());
        result.getOutputData().put("saved", true);
        return result;
    }
}
