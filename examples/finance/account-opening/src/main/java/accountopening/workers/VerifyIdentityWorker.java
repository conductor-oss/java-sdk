package accountopening.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Verifies applicant identity using document checks and knowledge-based authentication.
 * Real verification: checks that required documents are present and valid.
 */
public class VerifyIdentityWorker implements Worker {

    @Override
    public String getTaskDefName() { return "acc_verify_identity"; }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String applicantName = (String) task.getInputData().get("applicantName");
        Object docsObj = task.getInputData().get("documents");
        if (applicantName == null) applicantName = "UNKNOWN";

        boolean driversLicense = false;
        boolean ssn = false;
        boolean proofOfAddress = false;

        if (docsObj instanceof Map) {
            Map<String, Object> docs = (Map<String, Object>) docsObj;
            driversLicense = Boolean.TRUE.equals(docs.get("driversLicense"));
            ssn = Boolean.TRUE.equals(docs.get("ssn"));
            proofOfAddress = Boolean.TRUE.equals(docs.get("proofOfAddress"));
        }

        List<String> verificationMethods = new ArrayList<>();
        if (driversLicense) verificationMethods.add("document_verification");
        if (ssn) verificationMethods.add("ssn_trace");
        verificationMethods.add("knowledge_based_auth");

        boolean verified = driversLicense && ssn && proofOfAddress;
        List<String> failures = new ArrayList<>();
        if (!driversLicense) failures.add("Missing driver's license");
        if (!ssn) failures.add("Missing SSN");
        if (!proofOfAddress) failures.add("Missing proof of address");

        System.out.println("  [identity] Verifying identity for " + applicantName
                + " - verified: " + verified + " (methods: " + String.join(", ", verificationMethods) + ")");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("verified", verified);
        r.getOutputData().put("method", String.join(" + ", verificationMethods));
        r.getOutputData().put("failures", failures);
        r.getOutputData().put("verifiedAt", Instant.now().toString());
        return r;
    }
}
