package performancetesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RunLoadTestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pt_run_load_test";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [load] Running at 1000 RPS for 5m");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("run_load_test", true);
        result.addOutputData("processed", true);
        return result;
    }
}
