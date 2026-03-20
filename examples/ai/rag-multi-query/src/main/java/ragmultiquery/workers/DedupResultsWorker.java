package ragmultiquery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Worker that deduplicates search results from multiple query branches.
 * Takes results1, results2, results3 and returns unique documents by id.
 */
public class DedupResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mq_dedup_results";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, String>> results1 = (List<Map<String, String>>) task.getInputData().get("results1");
        List<Map<String, String>> results2 = (List<Map<String, String>>) task.getInputData().get("results2");
        List<Map<String, String>> results3 = (List<Map<String, String>>) task.getInputData().get("results3");

        if (results1 == null) results1 = List.of();
        if (results2 == null) results2 = List.of();
        if (results3 == null) results3 = List.of();

        List<Map<String, String>> all = new ArrayList<>();
        all.addAll(results1);
        all.addAll(results2);
        all.addAll(results3);

        Set<String> seen = new HashSet<>();
        List<String> uniqueDocs = new ArrayList<>();
        for (Map<String, String> doc : all) {
            String id = doc.get("id");
            if (!seen.contains(id)) {
                seen.add(id);
                uniqueDocs.add(doc.get("text"));
            }
        }

        System.out.println("  [dedup] " + all.size() + " total results -> " + uniqueDocs.size() + " unique docs");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("uniqueDocs", uniqueDocs);
        result.getOutputData().put("uniqueCount", uniqueDocs.size());
        result.getOutputData().put("totalRetrieved", all.size());
        return result;
    }
}
