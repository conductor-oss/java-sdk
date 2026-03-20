package datacompression.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Reports compression savings.
 * Input: originalSize (int), compressedSize (int), algorithm (string), verified (boolean)
 * Output: compressionRatio (string), bytesSaved (int), summary (string)
 */
public class ReportSavingsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmp_report_savings";
    }

    @Override
    public TaskResult execute(Task task) {
        Object origObj = task.getInputData().get("originalSize");
        int orig = origObj instanceof Number ? ((Number) origObj).intValue() : 1;
        if (orig == 0) orig = 1;
        Object compObj = task.getInputData().get("compressedSize");
        int comp = compObj instanceof Number ? ((Number) compObj).intValue() : 0;
        String algorithm = (String) task.getInputData().getOrDefault("algorithm", "unknown");
        Object verifiedObj = task.getInputData().get("verified");
        boolean verified = Boolean.TRUE.equals(verifiedObj);

        double ratioPercent = (1.0 - (double) comp / orig) * 100.0;
        String ratioStr = String.format("%.1f%%", ratioPercent);
        int saved = orig - comp;

        String summary = "Compression: " + algorithm + ", " + orig + " -> " + comp
                + " bytes, " + ratioStr + " savings (" + saved + " bytes saved), verified=" + verified;

        System.out.println("  [report] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("compressionRatio", ratioStr);
        result.getOutputData().put("bytesSaved", saved);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
