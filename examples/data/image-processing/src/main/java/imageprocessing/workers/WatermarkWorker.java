package imageprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Applies a watermark to an image.
 * Input: imageData, text
 * Output: result, applied, position, opacity
 */
public class WatermarkWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_watermark";
    }

    @Override
    public TaskResult execute(Task task) {
        String text = (String) task.getInputData().get("text");
        if (text == null || text.isEmpty()) {
            text = "Sample";
        }

        System.out.println("  [watermark] Applied watermark \"" + text + "\" at bottom-right with 50% opacity");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "watermarked");
        result.getOutputData().put("applied", true);
        result.getOutputData().put("position", "bottom-right");
        result.getOutputData().put("opacity", 0.5);
        return result;
    }
}
