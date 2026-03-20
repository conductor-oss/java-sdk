package datacompression.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.UUID;

/**
 * Compresses data with the chosen algorithm.
 * Input: records (list), algorithm (string), originalSize (int)
 * Output: compressedSize (int), checksum (string), algorithm (string)
 */
public class CompressDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmp_compress_data";
    }

    @Override
    public TaskResult execute(Task task) {
        Object origSizeObj = task.getInputData().get("originalSize");
        int originalSize = origSizeObj instanceof Number ? ((Number) origSizeObj).intValue() : 0;
        String algorithm = (String) task.getInputData().getOrDefault("algorithm", "gzip");

        double ratio;
        switch (algorithm) {
            case "lz4":
                ratio = 0.6;
                break;
            case "zstd":
                ratio = 0.25;
                break;
            default:
                ratio = 0.35;
                break;
        }

        int compressedSize = (int) Math.floor(originalSize * ratio);
        String checksum = "sha256:" + UUID.randomUUID().toString().substring(0, 16);

        System.out.println("  [compress] " + algorithm + ": " + originalSize + " -> " + compressedSize
                + " bytes (" + Math.round(ratio * 100) + "% of original)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("compressedSize", compressedSize);
        result.getOutputData().put("checksum", checksum);
        result.getOutputData().put("algorithm", algorithm);
        return result;
    }
}
