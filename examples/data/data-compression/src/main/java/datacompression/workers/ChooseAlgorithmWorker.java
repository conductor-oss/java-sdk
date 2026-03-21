package datacompression.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Chooses a compression algorithm based on data size.
 * Input: originalSizeBytes (int), recordCount (int), targetFormat (string)
 * Output: algorithm (string), estimatedRatio (double)
 */
public class ChooseAlgorithmWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmp_choose_algorithm";
    }

    @Override
    public TaskResult execute(Task task) {
        Object sizeObj = task.getInputData().get("originalSizeBytes");
        int size = sizeObj instanceof Number ? ((Number) sizeObj).intValue() : 0;

        String algorithm;
        double estimatedRatio;
        if (size < 1000) {
            algorithm = "lz4";
            estimatedRatio = 0.6;
        } else if (size < 100000) {
            algorithm = "gzip";
            estimatedRatio = 0.35;
        } else {
            algorithm = "zstd";
            estimatedRatio = 0.25;
        }

        System.out.println("  [algorithm] Selected \"" + algorithm + "\" for " + size
                + " bytes (estimated ratio: " + Math.round(estimatedRatio * 100) + "%)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("algorithm", algorithm);
        result.getOutputData().put("estimatedRatio", estimatedRatio);
        return result;
    }
}
