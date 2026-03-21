package tooluseconditional.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.regex.Pattern;

/**
 * Classifies a user query into one of three categories (math, code, search)
 * and routes it to the appropriate downstream tool.
 *
 * Output fields: category, toolName, parsedExpression, codeRequest, language, searchQuery.
 */
public class ClassifyQueryWorker implements Worker {

    private static final Pattern MATH_PATTERN = Pattern.compile(
            "(\\d+\\s*[+\\-*/^]\\s*\\d+|sqrt|square root|factorial|calculate|compute)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CODE_PATTERN = Pattern.compile(
            "(\\bwrite\\b|\\bcode\\b|\\bfunction\\b|\\bprogram\\b|\\bscript\\b|\\bimplement\\b|\\bdebug\\b|\\balgorithm\\b)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String getTaskDefName() {
        return "tc_classify_query";
    }

    @Override
    public TaskResult execute(Task task) {
        String userQuery = (String) task.getInputData().get("userQuery");
        if (userQuery == null || userQuery.isBlank()) {
            userQuery = "unknown query";
        }

        System.out.println("  [tc_classify_query] Classifying query: " + userQuery);

        String category;
        String toolName;
        String parsedExpression = "";
        String codeRequest = "";
        String language = "";
        String searchQuery = "";

        if (MATH_PATTERN.matcher(userQuery).find()) {
            category = "math";
            toolName = "calculator";
            parsedExpression = userQuery.replaceAll("(?i)(calculate|compute)\\s*", "").trim();
        } else if (CODE_PATTERN.matcher(userQuery).find()) {
            category = "code";
            toolName = "interpreter";
            codeRequest = userQuery;
            language = detectLanguage(userQuery);
        } else {
            category = "search";
            toolName = "web_search";
            searchQuery = userQuery;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("category", category);
        result.getOutputData().put("toolName", toolName);
        result.getOutputData().put("parsedExpression", parsedExpression);
        result.getOutputData().put("codeRequest", codeRequest);
        result.getOutputData().put("language", language);
        result.getOutputData().put("searchQuery", searchQuery);
        return result;
    }

    private String detectLanguage(String query) {
        String lower = query.toLowerCase();
        if (lower.contains("javascript") || lower.contains("js ") || lower.contains("node")) {
            return "javascript";
        }
        return "python";
    }
}
