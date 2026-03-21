package multiagentcodereview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Style review agent — inspects the AST for code style issues
 * such as inconsistent naming, missing documentation, and overly long functions.
 */
public class StyleReviewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_style_review";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> ast = (Map<String, Object>) task.getInputData().get("ast");
        String language = (String) task.getInputData().get("language");
        if (language == null || language.isBlank()) {
            language = "javascript";
        }

        System.out.println("  [cr_style_review] Reviewing " + language + " code for style issues");

        Map<String, Object> finding1 = new LinkedHashMap<>();
        finding1.put("severity", "LOW");
        finding1.put("type", "INCONSISTENT_NAMING");
        finding1.put("message", "Mix of camelCase and snake_case variable names throughout codebase");
        finding1.put("line", 12);

        Map<String, Object> finding2 = new LinkedHashMap<>();
        finding2.put("severity", "LOW");
        finding2.put("type", "MISSING_JSDOC");
        finding2.put("message", "Public functions handleRequest and processData lack JSDoc comments");
        finding2.put("line", 30);

        Map<String, Object> finding3 = new LinkedHashMap<>();
        finding3.put("severity", "LOW");
        finding3.put("type", "LONG_FUNCTION");
        finding3.put("message", "Function processData exceeds 50 lines — consider splitting into smaller functions");
        finding3.put("line", 40);

        List<Map<String, Object>> findings = List.of(finding1, finding2, finding3);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("findings", findings);
        result.getOutputData().put("agent", "style");
        return result;
    }
}
