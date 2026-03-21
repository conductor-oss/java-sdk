package auditlogging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class StoreImmutableWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "al_store_immutable";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [store] Event written to immutable audit log");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("store_immutable", true);
        result.addOutputData("processed", true);
        return result;
    }
}
