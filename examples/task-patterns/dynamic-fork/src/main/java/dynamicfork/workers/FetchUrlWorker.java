package dynamicfork.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Fetches a URL and returns metadata.
 *
 * In a real application, this would make an HTTP request.
 * Here it returns deterministic data based on the URL and index.
 */
public class FetchUrlWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "df_fetch_url";
    }

    @Override
    public TaskResult execute(Task task) {
        String url = (String) task.getInputData().get("url");
        Object indexObj = task.getInputData().get("index");
        int index = indexObj instanceof Number ? ((Number) indexObj).intValue() : 0;

        if (url == null || url.isBlank()) {
            url = "unknown";
        }

        System.out.println("  [df_fetch_url] Fetching URL #" + index + ": " + url);

        // Deterministic size and load time based on URL length and index
        int size = 1024 + (url.length() * 100) + (index * 256);
        int loadTime = 50 + (url.length() * 5) + (index * 10);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("url", url);
        result.getOutputData().put("status", 200);
        result.getOutputData().put("size", size);
        result.getOutputData().put("loadTime", loadTime);
        return result;
    }
}
