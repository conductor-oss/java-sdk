package imageprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Finalizes image processing by assembling all outputs.
 * Input: resized (list), watermarked, optimized
 * Output: outputUrl, totalOutputs
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ip_finalize";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<?> variants = (List<?>) task.getInputData().get("resized");
        if (variants == null) {
            variants = List.of();
        }

        System.out.println("  [finalize] Assembled " + variants.size() + " resized variants + watermark + optimized original");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("outputUrl", "s3://images/processed/output-bundle.zip");
        result.getOutputData().put("totalOutputs", variants.size() + 2);
        return result;
    }
}
