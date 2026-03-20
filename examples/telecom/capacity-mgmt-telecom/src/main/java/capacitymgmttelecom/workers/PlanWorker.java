package capacitymgmttelecom.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PlanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cmt_plan";
    }

    @Override
    public TaskResult execute(Task task) {

        String region = (String) task.getInputData().get("region");
        System.out.printf("  [plan] Expansion plan: add 2 cell towers in %s%n", region);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("plan", java.util.Map.of("towers", 2, "cost", 450000, "timeline", "6 weeks"));
        return result;
    }
}
