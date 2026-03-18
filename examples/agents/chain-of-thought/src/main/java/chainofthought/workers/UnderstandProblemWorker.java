package chainofthought.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Analyzes the incoming problem and produces a structured understanding
 * including the problem type and known values.
 */
public class UnderstandProblemWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ct_understand_problem";
    }

    @Override
    public TaskResult execute(Task task) {
        String problem = (String) task.getInputData().get("problem");
        if (problem == null || problem.isBlank()) {
            problem = "";
        }

        System.out.println("  [ct_understand_problem] Analyzing problem: " + problem);

        Map<String, Object> knownValues = Map.of(
                "principal", 10000,
                "rate", 0.05,
                "years", 3
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("understanding",
                "Calculate compound interest on a $10,000 principal at 5% annual rate for 3 years");
        result.getOutputData().put("type", "financial_calculation");
        result.getOutputData().put("knownValues", knownValues);
        return result;
    }
}
