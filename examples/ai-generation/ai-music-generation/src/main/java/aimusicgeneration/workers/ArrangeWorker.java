package aimusicgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ArrangeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "amg_arrange";
    }

    @Override
    public TaskResult execute(Task task) {

        String composition = (String) task.getInputData().get("composition");
        String durationSec = (String) task.getInputData().get("durationSec");
        System.out.println("  [arrange] Arranged for piano, strings, drums");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("arrangement", "arranged-3-instruments");
        result.getOutputData().put("actualDuration", 180);
        result.getOutputData().put("instruments", 3);
        return result;
    }
}
