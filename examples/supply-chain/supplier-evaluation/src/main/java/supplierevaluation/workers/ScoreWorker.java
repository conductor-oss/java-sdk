package supplierevaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
import java.util.stream.*;

public class ScoreWorker implements Worker {
    @Override public String getTaskDefName() { return "spe_score"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> suppliers = (List<Map<String, Object>>) task.getInputData().get("suppliers");
        if (suppliers == null) suppliers = List.of();
        List<Map<String, Object>> scores = suppliers.stream().map(s -> {
            double score = Math.round((((Number)s.get("deliveryOnTime")).doubleValue() * 40
                + ((Number)s.get("qualityRate")).doubleValue() * 40
                + (2 - ((Number)s.get("priceIndex")).doubleValue()) * 20) * 100.0) / 100.0;
            return Map.<String, Object>of("name", s.get("name"), "score", score);
        }).collect(Collectors.toList());
        System.out.println("  [score] Scored " + scores.size() + " suppliers");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("scores", scores);
        return r;
    }
}
