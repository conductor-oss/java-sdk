package supplierevaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
import java.util.stream.*;

public class RankWorker implements Worker {
    @Override public String getTaskDefName() { return "spe_rank"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> scores = (List<Map<String, Object>>) task.getInputData().get("scores");
        if (scores == null) scores = List.of();
        List<Map<String, Object>> sorted = new ArrayList<>(scores);
        sorted.sort((a, b) -> Double.compare(((Number)b.get("score")).doubleValue(), ((Number)a.get("score")).doubleValue()));
        List<Map<String, Object>> rankings = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Map<String, Object> m = new HashMap<>(sorted.get(i));
            m.put("rank", i + 1);
            rankings.add(m);
        }
        String top = rankings.isEmpty() ? "none" : (String) rankings.get(0).get("name");
        System.out.println("  [rank] Top supplier: " + top);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("rankings", rankings); r.getOutputData().put("topSupplier", top);
        return r;
    }
}
