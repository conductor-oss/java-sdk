package certificateissuance;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import certificateissuance.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 674: Certificate Issuance — Verify, Generate & Issue
 */
public class CertificateIssuanceExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 674: Certificate Issuance ===\n");

        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of(
                "cer_verify_completion", "cer_generate", "cer_sign", "cer_issue", "cer_record"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new VerifyCompletionWorker(), new GenerateCertificateWorker(),
                new SignCertificateWorker(), new IssueCertificateWorker(), new RecordCertificateWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("cer_certificate_issuance", 1,
                Map.of("studentId", "STU-2024-674", "courseId", "CS-301",
                       "courseName", "Machine Learning Fundamentals"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Completed: " + workflow.getOutput().get("completed"));
        System.out.println("  Certificate: " + workflow.getOutput().get("certificateId"));
        System.out.println("  Signed: " + workflow.getOutput().get("signed"));
        System.out.println("  Issued: " + workflow.getOutput().get("issued"));
        System.out.println("  Recorded: " + workflow.getOutput().get("recorded"));

        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
