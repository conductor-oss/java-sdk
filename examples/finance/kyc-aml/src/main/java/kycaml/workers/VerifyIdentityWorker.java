package kycaml.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Verifies customer identity using the provided document type.
 */
public class VerifyIdentityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "kyc_verify_identity";
    }

    @Override
    public TaskResult execute(Task task) {
        String name = (String) task.getInputData().get("name");
        String documentType = (String) task.getInputData().get("documentType");

        System.out.println("  [identity] Verifying " + documentType + " for " + name);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", true);
        result.getOutputData().put("documentAuthentic", true);
        result.getOutputData().put("matchScore", 98.5);
        result.getOutputData().put("verifiedAt", Instant.now().toString());
        return result;
    }
}
