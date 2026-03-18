package batchprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Prepares batches from input records by splitting them into chunks.
 * Input: records (list), batchSize (int)
 * Output: totalRecords, totalBatches, batchSize, batches (list of lists)
 */
public class PrepareBatchesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bp_prepare_batches";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Object> records = (List<Object>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        Object batchSizeObj = task.getInputData().get("batchSize");
        int batchSize = 3;
        if (batchSizeObj instanceof Number) {
            batchSize = ((Number) batchSizeObj).intValue();
        }
        if (batchSize <= 0) {
            batchSize = 3;
        }

        int totalRecords = records.size();
        int totalBatches = (int) Math.ceil((double) totalRecords / batchSize);

        // Actually split records into batch chunks
        List<List<Object>> batches = new ArrayList<>();
        for (int i = 0; i < totalRecords; i += batchSize) {
            int end = Math.min(i + batchSize, totalRecords);
            batches.add(new ArrayList<>(records.subList(i, end)));
        }

        System.out.println("  [prepare] " + totalRecords + " records -> " + totalBatches + " batches of " + batchSize);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("totalRecords", totalRecords);
        result.getOutputData().put("totalBatches", totalBatches);
        result.getOutputData().put("batchSize", batchSize);
        result.getOutputData().put("batches", batches);
        return result;
    }
}
