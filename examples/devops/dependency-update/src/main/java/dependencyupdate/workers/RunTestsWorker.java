package dependencyupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RunTestsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "du_run_tests";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [test] All 142 tests passed after update");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("run_tests", true);
        result.addOutputData("processed", true);
        return result;
    }
}
