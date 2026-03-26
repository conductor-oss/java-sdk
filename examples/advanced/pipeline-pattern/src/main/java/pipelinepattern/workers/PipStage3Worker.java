package pipelinepattern.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PipStage3Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pip_stage_3";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [stage-3: enrich] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("data", java.util.Map.of("enriched", true, "region", "US-EAST", "currency", "USD"));
        result.getOutputData().put("stage", "enrich");
        return result;
    }
}