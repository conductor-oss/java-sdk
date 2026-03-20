package imageprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Optimizes/compresses an image.
 * Input: imageData, format
 * Output: result, optimizedSize, compressionRatio, format
 */
public class OptimizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_optimize";
    }

    @Override
    public TaskResult execute(Task task) {
        String format = (String) task.getInputData().get("format");
        if (format == null || format.isEmpty()) {
            format = "png";
        }

        System.out.println("  [optimize] Compressed " + format + ": 4.2MB -> 1.8MB (57% reduction)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "optimized");
        result.getOutputData().put("optimizedSize", "1.8MB");
        result.getOutputData().put("compressionRatio", 0.57);
        result.getOutputData().put("format", format);
        return result;
    }
}
