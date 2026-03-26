package hronboarding;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import hronboarding.workers.CreateProfileWorker;
import hronboarding.workers.ProvisionWorker;
import hronboarding.workers.AssignMentorWorker;
import hronboarding.workers.TrainingWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 605: HR Onboarding
 *
 * perform  new employee onboarding from profile creation through training.
 *
 * Run:
 *   java -jar target/hr-onboarding-1.0.0.jar
 */
public class HrOnboardingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 605: HR Onboarding ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "hro_create_profile", "hro_provision", "hro_assign_mentor", "hro_training"));
        System.out.println("  Registered: hro_create_profile, hro_provision, hro_assign_mentor, hro_training\n");

        System.out.println("Step 2: Registering workflow 'hro_hr_onboarding'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CreateProfileWorker(),
                new ProvisionWorker(),
                new AssignMentorWorker(),
                new TrainingWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("hro_hr_onboarding", 1,
                Map.of("employeeName", "John Doe",
                       "department", "Engineering",
                       "startDate", "2024-04-01"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Employee ID: " + workflow.getOutput().get("employeeId"));
        System.out.println("  Mentor: " + workflow.getOutput().get("mentorId"));
        System.out.println("  Training plan: " + workflow.getOutput().get("trainingPlan"));

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
