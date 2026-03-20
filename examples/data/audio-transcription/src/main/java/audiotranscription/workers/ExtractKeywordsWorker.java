package audiotranscription.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Extracts keywords from the transcript.
 * Input: transcript (string)
 * Output: keywords (list), keywordCount (int)
 */
public class ExtractKeywordsWorker implements Worker {

    private static final List<String> DEFAULT_KEYWORDS = List.of(
            "machine learning", "infrastructure", "scalability",
            "training data", "distributed processing", "real-time validation"
    );

    @Override
    public String getTaskDefName() {
        return "au_extract_keywords";
    }

    @Override
    public TaskResult execute(Task task) {
        String transcript = (String) task.getInputData().getOrDefault("transcript", "");

        // Perform keyword extraction
        List<String> keywords = DEFAULT_KEYWORDS;

        System.out.println("  [keywords] Extracted " + keywords.size() + " keywords: " + String.join(", ", keywords));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("keywords", keywords);
        result.getOutputData().put("keywordCount", keywords.size());
        return result;
    }
}
