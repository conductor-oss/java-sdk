package agentmemory.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads conversation history and user profile from memory for the given user.
 * Returns conversationHistory (list of entries), userProfile, and memoryEntries count.
 */
public class LoadMemoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "am_load_memory";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        String userMessage = (String) task.getInputData().get("userMessage");

        if (userId == null || userId.isBlank()) {
            userId = "unknown";
        }
        if (userMessage == null || userMessage.isBlank()) {
            userMessage = "";
        }

        System.out.println("  [am_load_memory] Loading memory for user: " + userId);

        List<Map<String, String>> conversationHistory = new ArrayList<>();

        Map<String, String> entry1 = new LinkedHashMap<>();
        entry1.put("role", "user");
        entry1.put("content", "Hello, I'm interested in machine learning.");
        entry1.put("timestamp", "2025-01-15T09:00:00Z");
        conversationHistory.add(entry1);

        Map<String, String> entry2 = new LinkedHashMap<>();
        entry2.put("role", "assistant");
        entry2.put("content", "Great! Machine learning is a fascinating field. What aspect interests you most?");
        entry2.put("timestamp", "2025-01-15T09:00:05Z");
        conversationHistory.add(entry2);

        Map<String, String> entry3 = new LinkedHashMap<>();
        entry3.put("role", "user");
        entry3.put("content", "I want to understand neural networks and deep learning.");
        entry3.put("timestamp", "2025-01-15T09:01:00Z");
        conversationHistory.add(entry3);

        Map<String, String> entry4 = new LinkedHashMap<>();
        entry4.put("role", "assistant");
        entry4.put("content", "Neural networks are the foundation of deep learning. Let me know what specific topic you'd like to explore.");
        entry4.put("timestamp", "2025-01-15T09:01:10Z");
        conversationHistory.add(entry4);

        Map<String, Object> userProfile = new LinkedHashMap<>();
        userProfile.put("name", userId);
        userProfile.put("expertise", "intermediate");
        userProfile.put("interests", "machine learning, neural networks");
        userProfile.put("lastSeen", "2025-01-15T09:01:10Z");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("conversationHistory", conversationHistory);
        result.getOutputData().put("userProfile", userProfile);
        result.getOutputData().put("memoryEntries", 4);
        return result;
    }
}
