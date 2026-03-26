package pipelinepattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PipStage1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pip_stage_1";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [stage-1: validate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("data", java.util.Map.of("validated", true, "input", task.getInputData().getOrDefault("input", "")));
        result.getOutputData().put("stage", "validate");
        return result;
    }
}