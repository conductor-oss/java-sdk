package priorauthorization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApproveWorker implements Worker {

    @Override
    public String getTaskDefName() { return "pa_approve"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [approve] Auth " + task.getInputData().get("authId")
                + " APPROVED for " + task.getInputData().get("validDays") + " days");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("approved", true);
        output.put("authNumber", "AUTH-APP-55012");
        output.put("validDays", task.getInputData().get("validDays"));
        result.setOutputData(output);
        return result;
    }
}
