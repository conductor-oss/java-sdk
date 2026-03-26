package eventmerge.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Merges events from three streams into a single list.
 * Input: streamA (list), streamB (list), streamC (list)
 * Output: merged (combined list), totalCount
 */
public class MergeStreamsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mg_merge_streams";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, String>> streamA = (List<Map<String, String>>) task.getInputData().get("streamA");
        List<Map<String, String>> streamB = (List<Map<String, String>>) task.getInputData().get("streamB");
        List<Map<String, String>> streamC = (List<Map<String, String>>) task.getInputData().get("streamC");

        if (streamA == null) streamA = Collections.emptyList();
        if (streamB == null) streamB = Collections.emptyList();
        if (streamC == null) streamC = Collections.emptyList();

        List<Map<String, String>> merged = new ArrayList<>();
        merged.addAll(streamA);
        merged.addAll(streamB);
        merged.addAll(streamC);

        int totalCount = merged.size();

        System.out.println("  [mg_merge_streams] Merged " + totalCount + " events from 3 streams");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("merged", merged);
        result.getOutputData().put("totalCount", totalCount);
        return result;
    }
}
