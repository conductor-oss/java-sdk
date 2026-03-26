package taxassessment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssessPropertyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "txa_assess_property";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [assess] Property assessed at $450,000");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assessedValue", 450000);
        result.getOutputData().put("taxRate", 0.012);
        return result;
    }
}
