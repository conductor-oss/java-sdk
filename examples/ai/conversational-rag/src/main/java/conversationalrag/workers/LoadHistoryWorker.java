package conversationalrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Worker that loads conversation history for a given session.
 * Uses a static in-memory ConcurrentHashMap as the session store.
 * In production this would load from Redis or a database.
 */
public class LoadHistoryWorker implements Worker {

    /** Shared session store across all workers in this JVM. */
    static final ConcurrentHashMap<String, List<Map<String, String>>> SESSION_STORE =
            new ConcurrentHashMap<>();

    @Override
    public String getTaskDefName() {
        return "crag_load_history";
    }

    @Override
    public TaskResult execute(Task task) {
        String sessionId = (String) task.getInputData().get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "default";
        }

        List<Map<String, String>> history = SESSION_STORE.getOrDefault(sessionId, new ArrayList<>());

        System.out.println("  [history] Session \"" + sessionId + "\": " + history.size() + " previous turns");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("history", history);
        result.getOutputData().put("turnCount", history.size());
        return result;
    }
}
