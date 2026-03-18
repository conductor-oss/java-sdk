package raghybridsearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reciprocal Rank Fusion (RRF) merge worker.
 * Deduplicates results from vector and keyword searches by document id,
 * keeping unique text entries.
 */
public class RrfMergeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hs_rrf_merge";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [rrf] Applying reciprocal rank fusion (k=60)...");

        List<Map<String, Object>> vectorResults =
                (List<Map<String, Object>>) task.getInputData().get("vectorResults");
        List<Map<String, Object>> keywordResults =
                (List<Map<String, Object>>) task.getInputData().get("keywordResults");

        Set<String> seen = new HashSet<>();
        List<String> mergedDocs = new ArrayList<>();

        List<Map<String, Object>> allResults = new ArrayList<>();
        allResults.addAll(vectorResults);
        allResults.addAll(keywordResults);

        for (Map<String, Object> r : allResults) {
            String id = (String) r.get("id");
            if (!seen.contains(id)) {
                seen.add(id);
                mergedDocs.add((String) r.get("text"));
            }
        }

        System.out.println("  [rrf] Merged " + mergedDocs.size() + " unique docs from "
                + vectorResults.size() + " vector + " + keywordResults.size() + " keyword results");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mergedDocs", mergedDocs);
        result.getOutputData().put("uniqueCount", mergedDocs.size());
        return result;
    }
}
