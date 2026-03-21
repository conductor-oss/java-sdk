package databaseintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies source and target row counts match.
 * Input: sourceCount, writtenCount
 * Output: verified
 */
public class VerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dbi_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        Object sourceCount = task.getInputData().get("sourceCount");
        Object writtenCount = task.getInputData().get("writtenCount");
        boolean verified = String.valueOf(sourceCount).equals(String.valueOf(writtenCount));
        System.out.println("  [verify] Source: " + sourceCount + ", Written: " + writtenCount + ", Match: " + verified);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", verified);
        return result;
    }
}
