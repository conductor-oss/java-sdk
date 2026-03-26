package xmlparsing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Emits the final parsed records with a completion status.
 * Input: jsonRecords, recordCount
 * Output: status ("XML_TO_JSON_COMPLETE"), recordCount
 */
public class EmitRecordsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xp_emit_records";
    }

    @Override
    public TaskResult execute(Task task) {
        Object recordCount = task.getInputData().get("recordCount");
        if (recordCount == null) {
            recordCount = 0;
        }

        System.out.println("  [xp_emit_records] Emitting " + recordCount + " records");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "XML_TO_JSON_COMPLETE");
        result.getOutputData().put("recordCount", recordCount);
        return result;
    }
}
