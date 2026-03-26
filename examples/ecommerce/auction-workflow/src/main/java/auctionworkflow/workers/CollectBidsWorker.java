package auctionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class CollectBidsWorker implements Worker {
    @Override public String getTaskDefName() { return "auc_collect_bids"; }
    @Override public TaskResult execute(Task task) {
        int startPrice = 100; Object sp = task.getInputData().get("startingPrice"); if (sp instanceof Number) startPrice = ((Number)sp).intValue();
        List<Map<String, Object>> bids = List.of(
            Map.of("bidderId", "BID-001", "amount", startPrice + 10),
            Map.of("bidderId", "BID-002", "amount", startPrice + 25),
            Map.of("bidderId", "BID-003", "amount", startPrice + 50),
            Map.of("bidderId", "BID-001", "amount", startPrice + 65),
            Map.of("bidderId", "BID-004", "amount", startPrice + 80));
        System.out.println("  [bids] Collected " + bids.size() + " bids, highest: $" + (startPrice + 80));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("bids", bids); o.put("bidCount", bids.size());
        r.setOutputData(o); return r;
    }
}
