package imageprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Loads an image from a URL.
 * Input: imageUrl
 * Output: imageData, width, height, originalSize, format
 */
public class LoadImageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_load_image";
    }

    @Override
    public TaskResult execute(Task task) {
        String url = (String) task.getInputData().get("imageUrl");
        if (url == null || url.isEmpty()) {
            url = "unknown.jpg";
        }

        System.out.println("  [load] Loaded image from " + url + " -- 3840x2160, 4.2MB, PNG");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("imageData", "base64_deterministic_data");
        result.getOutputData().put("width", 3840);
        result.getOutputData().put("height", 2160);
        result.getOutputData().put("originalSize", "4.2MB");
        result.getOutputData().put("format", "png");
        return result;
    }
}
