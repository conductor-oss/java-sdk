package subscriptionbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class CheckPeriodWorker implements Worker {

    @Override
    public String getTaskDefName() { return "sub_check_period"; }

    @Override
    public TaskResult execute(Task task) {
        LocalDate now = LocalDate.now();
        String periodStart = now.withDayOfMonth(1).toString();
        String periodEnd = now.plusMonths(1).withDayOfMonth(1).toString();

        System.out.println("  [period] Subscription " + task.getInputData().get("subscriptionId")
                + ": " + periodStart + " to " + periodEnd + " (" + task.getInputData().get("billingCycle") + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("periodStart", periodStart);
        output.put("periodEnd", periodEnd);
        output.put("dueDate", periodStart);
        output.put("billingCycle", task.getInputData().get("billingCycle"));
        result.setOutputData(output);
        return result;
    }
}
