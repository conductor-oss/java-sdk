package reservationsystem;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import reservationsystem.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 736: Reservation System — Check Availability, Book, Confirm, Remind, Seat
 */
public class ReservationSystemExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 736: Reservation System ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("rsv_check_availability", "rsv_book", "rsv_confirm", "rsv_remind", "rsv_seat"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CheckAvailabilityWorker(), new BookWorker(), new ConfirmWorker(), new RemindWorker(), new SeatWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("reservation_system_736", 1,
                Map.of("guestName", "Johnson", "date", "2026-03-10", "time", "7:30 PM", "partySize", 4));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
