package fanoutfanin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes a single image and returns metadata.
 *
 * In a real application, this would perform actual image processing
 * (resize, compress, format conversion). Here it returns deterministic
 * compressed output: processedSize = originalSize / 3.
 */
public class ProcessImageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fo_process_image";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> image = (Map<String, Object>) task.getInputData().get("image");
        Object indexObj = task.getInputData().get("index");
        int index = indexObj instanceof Number ? ((Number) indexObj).intValue() : 0;

        String name = "unknown";
        int originalSize = 0;

        if (image != null) {
            Object nameObj = image.get("name");
            if (nameObj != null) {
                name = nameObj.toString();
            }
            Object sizeObj = image.get("size");
            if (sizeObj instanceof Number) {
                originalSize = ((Number) sizeObj).intValue();
            }
        }

        System.out.println("  [fo_process_image] Processing image #" + index + ": " + name);

        // Deterministic processed size: original / 3
        int processedSize = originalSize / 3;
        int processingTime = 50 + (name.length() * 5) + (index * 10);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("name", name);
        result.getOutputData().put("originalSize", originalSize);
        result.getOutputData().put("processedSize", processedSize);
        result.getOutputData().put("format", "webp");
        result.getOutputData().put("processingTime", processingTime);
        return result;
    }
}
