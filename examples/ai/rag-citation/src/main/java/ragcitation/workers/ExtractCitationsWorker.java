package ragcitation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker that extracts and validates citation markers from the generated answer.
 * Checks whether each citation marker actually appears in the answer text,
 * and reports the total count of citations found.
 */
public class ExtractCitationsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_extract_citations";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String answer = (String) task.getInputData().get("answer");
        List<Map<String, Object>> citations = (List<Map<String, Object>>) task.getInputData().get("citations");

        if (answer == null) {
            answer = "";
        }
        if (citations == null) {
            citations = List.of();
        }

        System.out.println("  [extract_citations] Extracting citations from answer");

        List<Map<String, Object>> extractedCitations = new ArrayList<>();
        int citationCount = 0;

        for (Map<String, Object> citation : citations) {
            String marker = (String) citation.get("marker");
            boolean foundInAnswer = answer.contains(marker);
            if (foundInAnswer) {
                citationCount++;
            }

            Map<String, Object> extracted = new HashMap<>();
            extracted.put("marker", marker);
            extracted.put("docId", citation.get("docId"));
            extracted.put("claim", citation.get("claim"));
            extracted.put("foundInAnswer", foundInAnswer);
            extractedCitations.add(extracted);
        }

        System.out.println("  [extract_citations] Found " + citationCount + " citations in answer text");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("extractedCitations", extractedCitations);
        result.getOutputData().put("citationCount", citationCount);
        return result;
    }
}
