package telemedicine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScheduleWorker implements Worker {

    @Override
    public String getTaskDefName() { return "tlm_schedule"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [schedule] Visit " + task.getInputData().get("visitId")
                + ": " + task.getInputData().get("reason"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("sessionUrl", "https://telehealth.example.com/session/TLM-9901");
        output.put("scheduledAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
