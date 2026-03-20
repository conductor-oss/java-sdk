package competitiveagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Practical solver — proposes an incremental process improvement.
 * Uses a "practical" approach with low cost, low risk, and modest innovation.
 */
public class Solver3Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "comp_solver_3";
    }

    @Override
    public TaskResult execute(Task task) {
        String problem = (String) task.getInputData().get("problem");
        if (problem == null || problem.isBlank()) {
            problem = "unspecified problem";
        }

        System.out.println("  [comp_solver_3] Practical solver analyzing: " + problem);

        Map<String, Object> solution = new LinkedHashMap<>();
        solution.put("approach", "practical");
        solution.put("title", "Incremental Process Improvement");
        solution.put("description", "A practical solution focusing on quick wins through incremental improvements to existing processes with minimal disruption.");
        solution.put("estimatedCost", "$45K");
        solution.put("timeline", "2 months");
        solution.put("innovationScore", 5);
        solution.put("riskLevel", "very-low");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("solution", solution);
        return result;
    }
}
