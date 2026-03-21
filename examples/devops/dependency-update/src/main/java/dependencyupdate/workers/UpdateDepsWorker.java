package dependencyupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class UpdateDepsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "du_update_deps";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [update] Updated 8 dependencies: 2 major, 3 minor, 3 patch");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("update_deps", true);
        result.addOutputData("processed", true);
        return result;
    }
}
