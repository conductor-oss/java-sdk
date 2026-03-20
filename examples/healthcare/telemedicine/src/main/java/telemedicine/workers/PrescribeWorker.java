package telemedicine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class PrescribeWorker implements Worker {

    @Override
    public String getTaskDefName() { return "tlm_prescribe"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [prescribe] Prescribing for: " + task.getInputData().get("diagnosis"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> rx = new LinkedHashMap<>();
        rx.put("medication", "Amoxicillin 500mg");
        rx.put("frequency", "3x daily");
        rx.put("duration", "7 days");
        output.put("prescription", rx);
        output.put("sentToPharmacy", true);
        result.setOutputData(output);
        return result;
    }
}
