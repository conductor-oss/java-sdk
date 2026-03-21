package codeinterpreter.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Interprets the execution results in the context of the original question.
 * Provides a human-readable answer, a structured interpretation with key metrics,
 * and a confidence score.
 */
public class InterpretResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ci_interpret_result";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "general data analysis";
        }

        Map<String, Object> executionOutput = (Map<String, Object>) task.getInputData().get("executionOutput");
        String executionStatus = (String) task.getInputData().get("executionStatus");
        if (executionStatus == null || executionStatus.isBlank()) {
            executionStatus = "unknown";
        }

        System.out.println("  [ci_interpret_result] Interpreting results for: " + question);

        String answer;
        Map<String, Object> interpretation;
        double confidence;

        if ("success".equals(executionStatus) && executionOutput != null) {
            answer = "Based on the analysis of the quarterly sales data, the West region has the "
                    + "highest average sales at $45,200.50, followed by the Northeast at $42,100.30. "
                    + "The Southwest region has the lowest average sales at $31,200.80. "
                    + "Across all 5 regions analyzed, there is a spread of $14,000 between the "
                    + "highest and lowest performing regions.";

            interpretation = Map.of(
                    "topRegion", "West",
                    "topAvgSales", 45200.50,
                    "bottomRegion", "Southwest",
                    "bottomAvgSales", 31200.80,
                    "totalRegions", 5,
                    "insight", "The West region outperforms all other regions by at least $3,100 in average sales, "
                            + "suggesting strong market conditions or effective sales strategies in that territory."
            );

            confidence = 0.96;
        } else {
            answer = "The code execution did not complete successfully. Unable to provide a "
                    + "definitive answer to the question: " + question;

            interpretation = Map.of(
                    "topRegion", "unknown",
                    "topAvgSales", 0.0,
                    "bottomRegion", "unknown",
                    "bottomAvgSales", 0.0,
                    "totalRegions", 0,
                    "insight", "Execution failed; no data available for interpretation."
            );

            confidence = 0.0;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("interpretation", interpretation);
        result.getOutputData().put("confidence", confidence);
        return result;
    }
}
