package ragcode.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Worker that parses a code-related question to extract intent, keywords, and language.
 * Returns a fixed parsed intent of "find_function_usage" with relevant keywords.
 */
public class ParseQueryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_parse_query";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null) {
            question = "";
        }

        String language = (String) task.getInputData().get("language");
        if (language == null) {
            language = "java";
        }

        System.out.println("  [parse_query] Parsing question: \"" + question + "\" for language: " + language);

        // Fixed deterministic parse result
        List<String> keywords = List.of("function", "usage", "example", language);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("intent", "find_function_usage");
        result.getOutputData().put("keywords", keywords);
        result.getOutputData().put("language", language);
        return result;
    }
}
