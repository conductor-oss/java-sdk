package multidocumentrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Worker that merges results from api_docs, tutorials, and forums collections,
 * sorts by score descending, and returns merged results with source counts.
 */
public class MergeResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mdrag_merge_results";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> apiDocs = getListInput(task, "apiDocs");
        List<Map<String, Object>> tutorials = getListInput(task, "tutorials");
        List<Map<String, Object>> forums = getListInput(task, "forums");

        List<Map<String, Object>> all = new ArrayList<>();
        all.addAll(apiDocs);
        all.addAll(tutorials);
        all.addAll(forums);
        all.sort(Comparator.comparingDouble(
                (Map<String, Object> m) -> ((Number) m.get("score")).doubleValue()).reversed());

        Map<String, Object> sourceCounts = Map.of(
                "api_docs", apiDocs.size(),
                "tutorials", tutorials.size(),
                "forums", forums.size()
        );

        System.out.println("  [merge] Combined " + all.size() + " results from 3 collections");
        System.out.println("    - api_docs: " + apiDocs.size()
                + ", tutorials: " + tutorials.size()
                + ", forums: " + forums.size());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("merged", all);
        result.getOutputData().put("sourceCounts", sourceCounts);
        result.getOutputData().put("totalResults", all.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getListInput(Task task, String key) {
        Object value = task.getInputData().get(key);
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return List.of();
    }
}
