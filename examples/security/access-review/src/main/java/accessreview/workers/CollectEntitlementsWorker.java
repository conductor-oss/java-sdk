package accessreview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectEntitlementsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ar_collect_entitlements";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [entitlements] engineering: 45 users, 312 entitlements collected");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("collect_entitlementsId", "COLLECT_ENTITLEMENTS-1300");
        result.addOutputData("success", true);
        return result;
    }
}
