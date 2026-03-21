package auctionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;

public class SettleAuctionWorker implements Worker {
    @Override public String getTaskDefName() { return "auc_settle"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [settle] Processing payment of $" + task.getInputData().get("winningBid") + " from " + task.getInputData().get("winnerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("settled", true); o.put("transactionId", "TXN-AUC-9901"); o.put("settledAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
