package datacompression.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies the integrity of compressed data.
 * Input: compressedSize (int), checksum (string), recordCount (int)
 * Output: integrityOk (boolean), checksum (string)
 */
public class VerifyIntegrityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmp_verify_integrity";
    }

    @Override
    public TaskResult execute(Task task) {
        String checksum = (String) task.getInputData().get("checksum");
        Object compSizeObj = task.getInputData().get("compressedSize");
        int compressedSize = compSizeObj instanceof Number ? ((Number) compSizeObj).intValue() : 0;

        boolean ok = checksum != null && !checksum.isEmpty() && compressedSize > 0;

        System.out.println("  [verify] Integrity check: checksum=" + (checksum != null ? "valid" : "missing")
                + ", size=" + compressedSize + " -> " + (ok ? "OK" : "FAILED"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("integrityOk", ok);
        result.getOutputData().put("checksum", checksum);
        return result;
    }
}
