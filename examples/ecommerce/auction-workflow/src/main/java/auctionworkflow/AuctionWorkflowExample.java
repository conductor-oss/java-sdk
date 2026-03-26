package auctionworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import auctionworkflow.workers.*;
import java.util.List;
import java.util.Map;

/** Example 468: Auction Workflow */
public class AuctionWorkflowExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 468: Auction Workflow ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("auc_open_bidding", "auc_collect_bids", "auc_close_auction", "auc_determine_winner", "auc_settle"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new OpenBiddingWorker(), new CollectBidsWorker(), new CloseAuctionWorker(), new DetermineWinnerWorker(), new SettleAuctionWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("auction_workflow", 1, Map.of("auctionId", "AUC-2024-001", "itemName", "Vintage Mechanical Watch", "startingPrice", 200));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 30000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
