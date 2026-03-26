package smarthome.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EvaluateRulesWorker implements Worker {
    @Override public String getTaskDefName() { return "smh_evaluate_rules"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [rules] Processing " + task.getInputData().getOrDefault("ruleId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("ruleId", "RULE-536-01");
        return r;
    }
}
