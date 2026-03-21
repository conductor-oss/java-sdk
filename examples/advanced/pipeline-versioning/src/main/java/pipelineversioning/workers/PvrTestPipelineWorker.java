package pipelineversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PvrTestPipelineWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pvr_test_pipeline";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [test] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allPassed", true);
        result.getOutputData().put("testCount", 4);
        return result;
    }
}