package jsontransformation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Emits the final transformed record.
 * Input: finalRecord (the validated nested record), isValid (boolean)
 * Output: record (the finalRecord as-is), emitted (true)
 */
public class EmitOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "jt_emit_output";
    }

    @Override
    public TaskResult execute(Task task) {
        Object finalRecord = task.getInputData().get("finalRecord");
        Object isValid = task.getInputData().get("isValid");

        System.out.println("  [jt_emit_output] Output record ready (valid: " + isValid + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("record", finalRecord);
        result.getOutputData().put("emitted", true);
        return result;
    }
}
