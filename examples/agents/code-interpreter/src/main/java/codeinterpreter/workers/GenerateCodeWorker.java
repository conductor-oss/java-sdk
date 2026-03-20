package codeinterpreter.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Generates Python code based on the analysis plan and data schema.
 * Produces a pandas-based script that performs group-by aggregation and sorting.
 * Returns the generated code, the language, and a line count.
 */
public class GenerateCodeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ci_generate_code";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> analysis = (Map<String, Object>) task.getInputData().get("analysis");
        Map<String, Object> dataSchema = (Map<String, Object>) task.getInputData().get("dataSchema");
        String language = (String) task.getInputData().get("language");
        if (language == null || language.isBlank()) {
            language = "python";
        }

        String targetColumn = "sales";
        String groupByColumn = "region";
        if (analysis != null) {
            if (analysis.get("targetColumn") != null) {
                targetColumn = (String) analysis.get("targetColumn");
            }
            if (analysis.get("groupByColumn") != null) {
                groupByColumn = (String) analysis.get("groupByColumn");
            }
        }

        System.out.println("  [ci_generate_code] Generating " + language + " code for analysis");

        String code = "import pandas as pd\n"
                + "\n"
                + "# Load dataset\n"
                + "df = pd.read_csv('dataset.csv')\n"
                + "\n"
                + "# Group by " + groupByColumn + " and calculate mean " + targetColumn + "\n"
                + "result = df.groupby('" + groupByColumn + "')['" + targetColumn + "'].mean()\n"
                + "\n"
                + "# Sort by " + targetColumn + " descending\n"
                + "result = result.sort_values(ascending=False)\n"
                + "\n"
                + "# Display results\n"
                + "print('Average " + targetColumn + " by " + groupByColumn + ":')\n"
                + "print(result.to_string())\n"
                + "print(f'\\nTop " + groupByColumn + ": {result.index[0]} with avg " + targetColumn + ": {result.iloc[0]:.2f}')\n";

        int linesOfCode = code.split("\n").length;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("code", code);
        result.getOutputData().put("language", language);
        result.getOutputData().put("linesOfCode", linesOfCode);
        return result;
    }
}
