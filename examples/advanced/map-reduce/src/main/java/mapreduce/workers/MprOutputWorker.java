package mapreduce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Output worker: formats the final map-reduce summary from the reduced result.
 *
 * Input: reducedResult (list), searchTerm
 * Output: summary (map with term, totalOccurrences, documentCount, topDocuments)
 */
public class MprOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mpr_output";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String searchTerm = (String) task.getInputData().getOrDefault("searchTerm", "");
        Object reducedObj = task.getInputData().get("reducedResult");

        int totalOccurrences = 0;
        int docCount = 0;

        if (reducedObj instanceof List) {
            List<Map<String, Object>> reduced = (List<Map<String, Object>>) reducedObj;
            docCount = reduced.size();
            for (Map<String, Object> entry : reduced) {
                Object countObj = entry.get("count");
                if (countObj instanceof Number) {
                    totalOccurrences += ((Number) countObj).intValue();
                }
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("term", searchTerm);
        summary.put("totalOccurrences", totalOccurrences);
        summary.put("documentsWithMatches", docCount);

        System.out.println("  [output] Summary: term=\"" + searchTerm + "\" total="
                + totalOccurrences + " docsWithMatches=" + docCount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
