package hotelbooking;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import hotelbooking.workers.*;
import java.util.List; import java.util.Map;
public class HotelBookingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 541: Hotel Booking ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("htl_search","htl_filter","htl_book","htl_confirm","htl_reminder"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new SearchWorker(),new FilterWorker(),new BookWorker(),new ConfirmWorker(),new ReminderWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("htl_hotel_booking", 1, Map.of("travelerId","TRV-400","city","New York","checkIn","2024-04-15","checkOut","2024-04-18"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Reservation ID: " + wf.getOutput().get("reservationId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
