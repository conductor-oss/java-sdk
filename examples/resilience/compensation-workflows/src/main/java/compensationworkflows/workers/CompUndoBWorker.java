package compensationworkflows.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for comp_undo_b -- reverses the action performed by Step B
 * by removing the record from the in-memory store.
 */
public class CompUndoBWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "comp_undo_b";
    }

    @Override
    public TaskResult execute(Task task) {
        Object original = task.getInputData().get("original");
        System.out.println("  [Undo B] Reversing: " + original);

        boolean removed = false;
        // Try to remove the record from the store
        if (original instanceof String) {
            String key = (String) original;
            String removedValue = CompStepBWorker.RECORDS.remove(key);
            removed = (removedValue != null);
            System.out.println("  [Undo B] Record removed: " + removed);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("undone", true);
        result.getOutputData().put("removed", removed);
        return result;
    }
}
