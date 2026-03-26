package codeinterpreter.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Analyzes a data question against a dataset schema to determine the type of analysis needed,
 * the operations required, and the target/groupBy columns. Returns analysis details, data schema,
 * and the recommended language (python).
 */
public class AnalyzeQuestionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ci_analyze_question";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "general data analysis";
        }

        Map<String, Object> dataset = (Map<String, Object>) task.getInputData().get("dataset");

        System.out.println("  [ci_analyze_question] Analyzing question: " + question);

        List<String> columns;
        if (dataset != null && dataset.get("columns") != null) {
            columns = (List<String>) dataset.get("columns");
        } else {
            columns = List.of("id", "value");
        }

        List<String> types = columns.stream()
                .map(col -> {
                    if ("sales".equals(col) || "units".equals(col) || "rows".equals(col)) {
                        return "numeric";
                    } else if ("region".equals(col) || "product".equals(col) || "quarter".equals(col)) {
                        return "categorical";
                    } else {
                        return "string";
                    }
                })
                .toList();

        Map<String, Object> analysis = Map.of(
                "type", "statistical_analysis",
                "operations", List.of("group_by", "mean", "sort"),
                "targetColumn", "sales",
                "groupByColumn", "region",
                "complexity", "medium"
        );

        Map<String, Object> dataSchema = Map.of(
                "columns", columns,
                "types", types
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("analysis", analysis);
        result.getOutputData().put("dataSchema", dataSchema);
        result.getOutputData().put("language", "python");
        return result;
    }
}
