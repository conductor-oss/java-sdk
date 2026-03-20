package realestatelisting;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import realestatelisting.workers.*;
import java.util.List;
import java.util.Map;
public class RealEstateListingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 681: Real Estate Listing ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rel_create","rel_verify","rel_enrich","rel_publish","rel_distribute"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CreateListingWorker(),new VerifyListingWorker(),new EnrichListingWorker(),new PublishListingWorker(),new DistributeListingWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("rel_real_estate_listing",1,Map.of("address","123 Oak Lane, Austin TX","price",475000,"agentId","AGT-100"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status);
        System.out.println("  Listing ID: "+wf.getOutput().get("listingId"));
        System.out.println("  Channels: "+wf.getOutput().get("channels"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
