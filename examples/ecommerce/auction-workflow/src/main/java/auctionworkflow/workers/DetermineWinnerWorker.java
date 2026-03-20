package auctionworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class DetermineWinnerWorker implements Worker {
    @Override public String getTaskDefName() { return "auc_determine_winner"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> bids = (List<Map<String, Object>>) task.getInputData().getOrDefault("bids", List.of());
        String winnerId = "none"; int winningBid = 0;
        for (Map<String, Object> bid : bids) {
            int amount = bid.get("amount") instanceof Number ? ((Number) bid.get("amount")).intValue() : 0;
            if (amount > winningBid) { winningBid = amount; winnerId = (String) bid.get("bidderId"); }
        }
        System.out.println("  [winner] Winner: " + winnerId + " with bid $" + winningBid);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("winnerId", winnerId); o.put("winningBid", winningBid);
        r.setOutputData(o); return r;
    }
}
