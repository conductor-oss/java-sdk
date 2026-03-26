package referralmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScheduleReferralWorker implements Worker {

    @Override
    public String getTaskDefName() { return "ref_schedule"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [schedule] Scheduling with " + task.getInputData().get("specialistId")
                + " for patient " + task.getInputData().get("patientId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("appointmentId", "APT-SPEC-7605");
        output.put("appointmentDate", "2024-03-25");
        output.put("appointmentTime", "2:00 PM");
        result.setOutputData(output);
        return result;
    }
}
