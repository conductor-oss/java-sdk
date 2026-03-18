package travelbooking;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import travelbooking.workers.*;
import java.util.List; import java.util.Map;
public class TravelBookingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 545: Travel Booking ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tvb_search","tvb_compare","tvb_book","tvb_confirm","tvb_itinerary"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new SearchWorker(),new CompareWorker(),new BookWorker(),new ConfirmWorker(),new ItineraryWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("tvb_travel_booking", 1, Map.of("travelerId","TRV-100","origin","SFO","destination","JFK","departDate","2024-04-15"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Booking ID: " + wf.getOutput().get("bookingId"));
        System.out.println("  Total cost: " + wf.getOutput().get("totalCost"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
