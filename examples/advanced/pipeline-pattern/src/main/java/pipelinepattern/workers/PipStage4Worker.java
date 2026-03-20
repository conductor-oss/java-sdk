package pipelinepattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PipStage4Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pip_stage_4";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [stage-4: output] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("data", java.util.Map.of("finalized", true, "outputTimestamp", java.time.Instant.now().toString()));
        result.getOutputData().put("stage", "output");
        return result;
    }
}