package performancereview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

public class SelfEvalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pfr_self_eval";
    }

    @Override
    public TaskResult execute(Task task) {
        String employeeId = (String) task.getInputData().get("employeeId");
        System.out.println("  [self-eval] Employee " + employeeId + " self-assessment: 4/5");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rating", 4);
        result.getOutputData().put("strengths", List.of("collaboration", "delivery"));
        result.getOutputData().put("areas", List.of("documentation"));
        return result;
    }
}
