package mapreduce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map worker 1: counts occurrences of the search term in each document
 * of its partition. Produces a list of {docId, count} pairs.
 *
 * Input: partition (list of text strings), searchTerm
 * Output: mapped (list of maps with "docIndex" and "count")
 */
public class MprMap1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mpr_map_1";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object partObj = task.getInputData().get("partition");
        String searchTerm = (String) task.getInputData().getOrDefault("searchTerm", "");

        List<Map<String, Object>> mapped = MapWorkerUtil.countInPartition(partObj, searchTerm, "map-1");

        System.out.println("  [map-1] Processed " + mapped.size() + " documents for term \"" + searchTerm + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("mapped", mapped);
        return result;
    }
}
