package competitiveagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Analytical solver — proposes a data-driven optimization framework.
 * Uses an "analytical" approach with balanced innovation, cost, and risk.
 */
public class Solver2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "comp_solver_2";
    }

    @Override
    public TaskResult execute(Task task) {
        String problem = (String) task.getInputData().get("problem");
        if (problem == null || problem.isBlank()) {
            problem = "unspecified problem";
        }

        System.out.println("  [comp_solver_2] Analytical solver analyzing: " + problem);

        Map<String, Object> solution = new LinkedHashMap<>();
        solution.put("approach", "analytical");
        solution.put("title", "Data-Driven Optimization Framework");
        solution.put("description", "An analytical solution using statistical modeling and data analysis to identify bottlenecks and optimize processes systematically.");
        solution.put("estimatedCost", "$85K");
        solution.put("timeline", "4 months");
        solution.put("innovationScore", 7);
        solution.put("riskLevel", "low");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("solution", solution);
        return result;
    }
}
