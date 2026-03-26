package sagaforkjoin;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import sagaforkjoin.workers.BookCarWorker;
import sagaforkjoin.workers.BookFlightWorker;
import sagaforkjoin.workers.BookHotelWorker;
import sagaforkjoin.workers.CheckResultsWorker;
import sagaforkjoin.workers.ConfirmAllWorker;

import java.util.List;
import java.util.Map;

/**
 * Parallel Bookings with Rollback (Saga FORK_JOIN)
 *
 * Demonstrates a saga pattern using Conductor's FORK_JOIN to book hotel,
 * flight, and car in parallel. After all branches complete (JOIN), the
 * workflow checks all results and confirms the trip.
 *
 * Run:
 *   java -jar target/saga-fork-join-1.0.0.jar
 *   java -jar target/saga-fork-join-1.0.0.jar --workers
 */
public class SagaForkJoinExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Parallel Bookings with Rollback (Saga FORK_JOIN) ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        List<TaskDef> taskDefs = List.of(
                createTaskDef("sfj_book_hotel"),
                createTaskDef("sfj_book_flight"),
                createTaskDef("sfj_book_car"),
                createTaskDef("sfj_check_results"),
                createTaskDef("sfj_confirm_all")
        );
        client.registerTaskDefs(taskDefs);

        System.out.println("  Registered 5 task definitions");
        System.out.println("    sfj_book_hotel, sfj_book_flight, sfj_book_car");
        System.out.println("    sfj_check_results, sfj_confirm_all\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'saga_fork_join_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new BookHotelWorker(),
                new BookFlightWorker(),
                new BookCarWorker(),
                new CheckResultsWorker(),
                new ConfirmAllWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow (tripId=TRIP-42)...\n");
        String workflowId = client.startWorkflow("saga_fork_join_demo", 1,
                Map.of("tripId", "TRIP-42"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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

    private static TaskDef createTaskDef(String name) {
        TaskDef def = new TaskDef();
        def.setName(name);
        def.setRetryCount(0);
        def.setTimeoutSeconds(60);
        def.setResponseTimeoutSeconds(30);
        def.setOwnerEmail("examples@orkes.io");
        return def;
    }
}
