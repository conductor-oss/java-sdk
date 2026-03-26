package carrental;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import carrental.workers.*;
import java.util.List; import java.util.Map;
public class CarRentalExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 542: Car Rental ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("crl_search","crl_select","crl_book","crl_pickup","crl_return"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new SearchWorker(),new SelectWorker(),new BookWorker(),new PickupWorker(),new ReturnWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("crl_car_rental", 1, Map.of("travelerId","TRV-500","location","LAX Airport","pickupDate","2024-04-15","returnDate","2024-04-18"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Reservation ID: " + wf.getOutput().get("reservationId"));
        System.out.println("  Total cost: " + wf.getOutput().get("totalCost"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
