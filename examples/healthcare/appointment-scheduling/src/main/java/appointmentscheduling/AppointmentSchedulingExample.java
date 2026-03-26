package appointmentscheduling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import appointmentscheduling.workers.CheckAvailabilityWorker;
import appointmentscheduling.workers.BookWorker;
import appointmentscheduling.workers.ConfirmWorker;
import appointmentscheduling.workers.RemindWorker;

import java.util.List;
import java.util.Map;

/**
 * Appointment Scheduling Demo
 *
 * apt_check_availability -> apt_book -> apt_confirm -> apt_remind
 *
 * Run:
 *   java -jar target/appointment-scheduling-1.0.0.jar
 */
public class AppointmentSchedulingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Appointment Scheduling Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "apt_check_availability", "apt_book", "apt_confirm", "apt_remind"));
        System.out.println("  Registered: apt_check_availability, apt_book, apt_confirm, apt_remind\n");

        System.out.println("Step 2: Registering workflow 'appointment_scheduling_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckAvailabilityWorker(),
                new BookWorker(),
                new ConfirmWorker(),
                new RemindWorker()
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
        String workflowId = client.startWorkflow("appointment_scheduling_workflow", 1,
                Map.of("patientId", "PAT-10234",
                        "providerId", "DR-CHEN-001",
                        "preferredDate", "2024-03-20",
                        "visitType", "follow-up"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

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
