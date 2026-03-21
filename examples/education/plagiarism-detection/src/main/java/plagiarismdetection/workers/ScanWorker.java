package plagiarismdetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class ScanWorker implements Worker {
    @Override public String getTaskDefName() { return "plg_scan"; }

    @Override public TaskResult execute(Task task) {
        String text = (String) task.getInputData().getOrDefault("documentText", "");
        int wordCount = text.isEmpty() ? 0 : text.split("\\s+").length;
        System.out.println("  [scan] Scanned " + wordCount + " words against database");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("scanResults", Map.of("wordCount", wordCount, "matchedSegments", 2));
        return result;
    }
}
