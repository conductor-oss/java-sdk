package datapartitioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes partition B by adding processed:true and partition:"B" to each record.
 *
 * Input:  partition (list of records), partitionName (string)
 * Output: result (list of processed records), processedCount (int)
 */
public class ProcessPartitionBWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "par_process_partition_b";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> partition =
                (List<Map<String, Object>>) task.getInputData().get("partition");

        if (partition == null) {
            partition = List.of();
        }

        List<Map<String, Object>> processed = new ArrayList<>();
        for (Map<String, Object> record : partition) {
            Map<String, Object> enriched = new HashMap<>(record);
            enriched.put("processed", true);
            enriched.put("partition", "B");
            processed.add(enriched);
        }

        System.out.println("  [par_process_partition_b] Processed " + processed.size() + " records in partition B");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", processed);
        result.getOutputData().put("processedCount", processed.size());
        return result;
    }
}
