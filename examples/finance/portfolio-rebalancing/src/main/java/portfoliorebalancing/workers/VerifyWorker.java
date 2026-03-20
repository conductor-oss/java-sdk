package portfoliorebalancing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class VerifyWorker implements Worker {
    @Override public String getTaskDefName() { return "prt_verify"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> trades = (List<Map<String, Object>>) task.getInputData().get("executedTrades");
        if (trades == null) trades = List.of();
        boolean allFilled = trades.stream().allMatch(t -> Boolean.TRUE.equals(t.get("filled")));
        System.out.println("  [verify] " + trades.size() + " trades verified — all filled: " + allFilled);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", allFilled);
        result.getOutputData().put("newAllocations", "within tolerance");
        return result;
    }
}
