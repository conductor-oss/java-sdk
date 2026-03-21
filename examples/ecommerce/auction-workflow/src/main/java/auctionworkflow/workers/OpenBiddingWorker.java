package auctionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;

public class OpenBiddingWorker implements Worker {
    @Override public String getTaskDefName() { return "auc_open_bidding"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [open] Auction " + task.getInputData().get("auctionId") + ": \"" + task.getInputData().get("itemName") + "\" starting at $" + task.getInputData().get("startingPrice"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("status", "open"); o.put("openedAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
