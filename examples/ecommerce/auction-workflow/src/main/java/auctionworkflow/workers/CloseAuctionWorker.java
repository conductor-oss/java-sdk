package auctionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;

public class CloseAuctionWorker implements Worker {
    @Override public String getTaskDefName() { return "auc_close_auction"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [close] Auction " + task.getInputData().get("auctionId") + " closed with " + task.getInputData().get("bidCount") + " bids");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("status", "closed"); o.put("closedAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
