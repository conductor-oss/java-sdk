package druginteraction.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class RecommendAlternativesWorker implements Worker {

    @Override
    public String getTaskDefName() { return "drg_recommend_alternatives"; }

    @Override
    public TaskResult execute(Task task) {
        List<?> conflicts = (List<?>) task.getInputData().getOrDefault("conflicts", List.of());
        String newMed = String.valueOf(task.getInputData().getOrDefault("newMedication", ""));
        List<Map<String, Object>> alternatives = new ArrayList<>();
        if (!conflicts.isEmpty()) {
            alternatives.add(Map.of("original", newMed, "alternative", "Acetaminophen", "reason", "No interaction with Warfarin"));
            alternatives.add(Map.of("original", newMed, "alternative", "Celecoxib (low dose)", "reason", "Lower bleeding risk"));
        }
        System.out.println("  [recommend] " + alternatives.size() + " alternative(s) suggested");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("alternatives", alternatives);
        output.put("reviewed", true);
        result.setOutputData(output);
        return result;
    }
}
