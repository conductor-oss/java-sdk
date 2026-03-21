package rfpautomation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class EvaluateWorker implements Worker {
    @Override public String getTaskDefName() { return "rfp_evaluate"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> proposals = (List<Map<String, Object>>) task.getInputData().get("proposals");
        if (proposals == null) proposals = List.of();
        Map<String, Object> top = proposals.stream()
            .max(Comparator.comparingInt(p -> ((Number) p.get("score")).intValue()))
            .orElse(Map.of("vendor","none"));
        System.out.println("  [evaluate] Top candidate: " + top.get("vendor") + " (score: " + top.get("score") + ")");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("topCandidate", top.get("vendor")); r.getOutputData().put("evaluationComplete", true); return r;
    }
}
