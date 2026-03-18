package compensationworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Worker for comp_step_b -- inserts a record into an in-memory store.
 * The record key is returned so the undo worker can remove it.
 */
public class CompStepBWorker implements Worker {

    // In-memory record store shared with UndoBWorker
    static final ConcurrentHashMap<String, String> RECORDS = new ConcurrentHashMap<>();

    @Override
    public String getTaskDefName() {
        return "comp_step_b";
    }

    @Override
    public TaskResult execute(Task task) {
        // Generate a record key based on timestamp
        String recordKey = "record-B-" + System.nanoTime();
        String recordValue = "Inserted at " + Instant.now();

        RECORDS.put(recordKey, recordValue);

        System.out.println("  [Step B] Executing -- record inserted: " + recordKey);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "record-B-inserted");
        result.getOutputData().put("recordKey", recordKey);
        return result;
    }

    /** Visible for testing. */
    static void clearRecords() {
        RECORDS.clear();
    }
}
