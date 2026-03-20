package servicemigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CutoverWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sm_cutover";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [cutover] Traffic switched to new environment");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("cutover", true);
        result.addOutputData("processed", true);
        return result;
    }
}
