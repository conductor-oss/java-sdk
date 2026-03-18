package sagapattern;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sagapattern.workers.ReserveHotelWorker;
import sagapattern.workers.BookFlightWorker;
import sagapattern.workers.ChargePaymentWorker;
import sagapattern.workers.CancelFlightWorker;
import sagapattern.workers.CancelHotelWorker;
import sagapattern.workers.RefundPaymentWorker;

import java.util.List;
import java.util.Map;

/**
 * Saga Pattern -- Orchestrated Compensation for Distributed Transactions
 *
 * Demonstrates the saga pattern where a trip booking workflow performs
 * forward steps (hotel, flight, payment) and compensating steps
 * (cancel flight, cancel hotel) if payment fails.
 *
 * Forward steps:
 *   saga_reserve_hotel -> saga_book_flight -> saga_charge_payment
 *
 * On payment failure (SWITCH on payment status):
 *   saga_cancel_flight -> saga_cancel_hotel -> TERMINATE(COMPLETED, {status:"ROLLED_BACK"})
 *
 * Run:
 *   java -jar target/saga-pattern-1.0.0.jar
 */
public class SagaPatternExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Saga Pattern: Orchestrated Compensation for Trip Booking ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "saga_reserve_hotel", "saga_book_flight", "saga_charge_payment",
                "saga_cancel_flight", "saga_cancel_hotel", "saga_refund_payment"));
        System.out.println("  Registered: saga_reserve_hotel, saga_book_flight, saga_charge_payment, saga_cancel_flight, saga_cancel_hotel, saga_refund_payment\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'trip_booking_saga'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReserveHotelWorker(),
                new BookFlightWorker(),
                new ChargePaymentWorker(),
                new CancelFlightWorker(),
                new CancelHotelWorker(),
                new RefundPaymentWorker());
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        boolean allPassed = true;

        // Scenario 1: Successful trip booking
        System.out.println("--- Scenario 1: Successful trip booking ---");
        String wfId1 = client.startWorkflow("trip_booking_saga", 1,
                Map.of("tripId", "TRIP-001", "shouldFail", false));
        System.out.println("  Workflow ID: " + wfId1);

        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        String status1 = wf1.getStatus().name();
        System.out.println("  Status: " + status1);
        System.out.println("  Output: " + wf1.getOutput());

        if (!"COMPLETED".equals(status1)
                || !"HTL-TRIP-001".equals(wf1.getOutput().get("hotel"))
                || !"FLT-TRIP-001".equals(wf1.getOutput().get("flight"))
                || !"TXN-TRIP-001".equals(wf1.getOutput().get("transactionId"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with hotel, flight, and payment details\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: trip booked successfully with all services confirmed.\n");
        }

        // Scenario 2: Payment failure triggers compensation
        System.out.println("--- Scenario 2: Payment failure triggers saga rollback ---");
        String wfId2 = client.startWorkflow("trip_booking_saga", 1,
                Map.of("tripId", "TRIP-002", "shouldFail", true));
        System.out.println("  Workflow ID: " + wfId2);

        Workflow wf2 = client.waitForWorkflow(wfId2, "COMPLETED", 30000);
        String status2 = wf2.getStatus().name();
        System.out.println("  Status: " + status2);
        System.out.println("  Output: " + wf2.getOutput());

        if (!"COMPLETED".equals(status2)
                || !"ROLLED_BACK".equals(wf2.getOutput().get("status"))) {
            System.out.println("  UNEXPECTED -- expected COMPLETED with status=ROLLED_BACK\n");
            allPassed = false;
        } else {
            System.out.println("  As expected: payment failed, saga compensated by cancelling flight and hotel.\n");
        }

        // Summary
        System.out.println("Key insight: The saga pattern uses orchestrated compensation --");
        System.out.println("when a step fails, the workflow runs compensating tasks in reverse order");
        System.out.println("to undo the effects of previously completed steps.");

        client.stopWorkers();

        if (allPassed) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
