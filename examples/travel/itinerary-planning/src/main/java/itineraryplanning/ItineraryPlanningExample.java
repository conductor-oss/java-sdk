package itineraryplanning;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import itineraryplanning.workers.*;
import java.util.List; import java.util.Map;
public class ItineraryPlanningExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 544: Itinerary Planning ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("itp_preferences","itp_search","itp_optimize","itp_book","itp_finalize"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new PreferencesWorker(),new SearchWorker(),new OptimizeWorker(),new BookWorker(),new FinalizeWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("itp_itinerary_planning", 1, Map.of("travelerId","TRV-200","destination","Chicago","days",3));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Itinerary ID: " + wf.getOutput().get("itineraryId"));
        System.out.println("  Total cost: " + wf.getOutput().get("totalCost"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
