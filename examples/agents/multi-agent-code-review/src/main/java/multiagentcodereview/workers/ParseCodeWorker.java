package multiagentcodereview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses submitted code and produces a simplified AST representation
 * including functions, imports, line count, and complexity score.
 */
public class ParseCodeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_parse_code";
    }

    @Override
    public TaskResult execute(Task task) {
        String code = (String) task.getInputData().get("code");
        if (code == null || code.isBlank()) {
            code = "// empty";
        }
        String language = (String) task.getInputData().get("language");
        if (language == null || language.isBlank()) {
            language = "javascript";
        }

        System.out.println("  [cr_parse_code] Parsing " + language + " code (" + code.length() + " chars)");

        Map<String, Object> ast = new LinkedHashMap<>();
        ast.put("functions", List.of("handleRequest", "validateInput", "processData", "sendResponse"));
        ast.put("imports", List.of("express", "crypto", "helmet", "pg"));
        ast.put("lines", 142);
        ast.put("complexity", 18);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("ast", ast);
        result.getOutputData().put("language", language);
        return result;
    }
}
