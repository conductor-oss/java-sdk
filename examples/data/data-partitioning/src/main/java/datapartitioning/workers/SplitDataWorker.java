package datapartitioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Splits a list of records into two partitions (A and B).
 * First ceil(n/2) records go to partition A, the rest to partition B.
 *
 * Input:  records (list), partitionKey (string)
 * Output: partitionA (list), partitionB (list), totalCount (int)
 */
public class SplitDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "par_split_data";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records =
                (List<Map<String, Object>>) task.getInputData().get("records");

        if (records == null) {
            records = List.of();
        }

        int total = records.size();
        int splitIndex = (int) Math.ceil(total / 2.0);

        List<Map<String, Object>> partitionA = records.subList(0, splitIndex);
        List<Map<String, Object>> partitionB = records.subList(splitIndex, total);

        String partitionKey = (String) task.getInputData().get("partitionKey");
        if (partitionKey == null) {
            partitionKey = "default";
        }

        System.out.println("  [par_split_data] Split " + total + " records by key '"
                + partitionKey + "' into A(" + partitionA.size() + ") and B(" + partitionB.size() + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("partitionA", partitionA);
        result.getOutputData().put("partitionB", partitionB);
        result.getOutputData().put("totalCount", total);
        return result;
    }
}
