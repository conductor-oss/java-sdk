package educationenrollment;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import educationenrollment.workers.ApplyWorker;
import educationenrollment.workers.ReviewWorker;
import educationenrollment.workers.AdmitWorker;
import educationenrollment.workers.EnrollWorker;
import educationenrollment.workers.OrientWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 671: Education Enrollment — Student Admission Pipeline
 *
 * Apply, review application, admit, enroll, and orient a new student.
 *
 * Run:
 *   java -jar target/education-enrollment-1.0.0.jar
 */
public class EducationEnrollmentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 671: Education Enrollment ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "edu_apply", "edu_review", "edu_admit", "edu_enroll", "edu_orient"));
        System.out.println("  Registered: edu_apply, edu_review, edu_admit, edu_enroll, edu_orient\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'edu_enrollment'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ApplyWorker(),
                new ReviewWorker(),
                new AdmitWorker(),
                new EnrollWorker(),
                new OrientWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("edu_enrollment", 1,
                Map.of("studentName", "Jane Smith",
                       "program", "Computer Science",
                       "gpa", 3.8));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Application: " + workflow.getOutput().get("applicationId"));
        System.out.println("  Admitted: " + workflow.getOutput().get("admitted"));
        System.out.println("  Student ID: " + workflow.getOutput().get("studentId"));
        System.out.println("  Enrolled: " + workflow.getOutput().get("enrolled"));
        System.out.println("  Orientation: " + workflow.getOutput().get("orientationDate"));

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
