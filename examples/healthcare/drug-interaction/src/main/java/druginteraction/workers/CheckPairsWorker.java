package druginteraction.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class CheckPairsWorker implements Worker {

    @Override
    public String getTaskDefName() { return "drg_check_pairs"; }

    @Override
    public TaskResult execute(Task task) {
        List<?> meds = (List<?>) task.getInputData().getOrDefault("medications", List.of());
        String newMed = String.valueOf(task.getInputData().getOrDefault("newMedication", ""));
        System.out.println("  [check] Checking " + newMed + " against " + meds.size() + " current medications");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        List<Map<String, Object>> interactions = new ArrayList<>();
        interactions.add(Map.of("drug1", "Warfarin", "drug2", newMed, "severity", "major", "effect", "Increased bleeding risk"));
        interactions.add(Map.of("drug1", "Lisinopril", "drug2", newMed, "severity", "none", "effect", "No interaction"));
        interactions.add(Map.of("drug1", "Metformin", "drug2", newMed, "severity", "minor", "effect", "Mild GI upset possible"));
        output.put("interactions", interactions);
        output.put("pairsChecked", meds.size());
        result.setOutputData(output);
        return result;
    }
}
