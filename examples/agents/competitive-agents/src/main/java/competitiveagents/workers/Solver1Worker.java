package competitiveagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creative solver — proposes an innovative, AI-powered adaptive solution.
 * Uses a "creative" approach with higher innovation but higher cost and risk.
 */
public class Solver1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "comp_solver_1";
    }

    @Override
    public TaskResult execute(Task task) {
        String problem = (String) task.getInputData().get("problem");
        if (problem == null || problem.isBlank()) {
            problem = "unspecified problem";
        }

        System.out.println("  [comp_solver_1] Creative solver analyzing: " + problem);

        Map<String, Object> solution = new LinkedHashMap<>();
        solution.put("approach", "creative");
        solution.put("title", "AI-Powered Adaptive System");
        solution.put("description", "A creative solution leveraging AI and machine learning to dynamically adapt to changing conditions and optimize outcomes in real-time.");
        solution.put("estimatedCost", "$120K");
        solution.put("timeline", "6 months");
        solution.put("innovationScore", 9);
        solution.put("riskLevel", "medium-high");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("solution", solution);
        return result;
    }
}
