package bidmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class EvaluateWorker implements Worker {
    @Override public String getTaskDefName() { return "bid_evaluate"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> responses = (List<Map<String, Object>>) task.getInputData().get("responses");
        if (responses == null) responses = List.of();
        Map<String, Object> winner = responses.stream()
            .min(Comparator.comparingInt(r -> ((Number) r.get("amount")).intValue()))
            .orElse(Map.of("vendor","none","amount",0));
        System.out.println("  [evaluate] Best bid: " + winner.get("vendor") + " at $" + winner.get("amount"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("winner", winner.get("vendor")); r.getOutputData().put("winningBid", winner.get("amount")); return r;
    }
}
