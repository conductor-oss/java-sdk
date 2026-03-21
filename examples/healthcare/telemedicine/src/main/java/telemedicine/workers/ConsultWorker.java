package telemedicine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConsultWorker implements Worker {

    @Override
    public String getTaskDefName() { return "tlm_consult"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [consult] Consultation for: " + task.getInputData().get("reason"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("diagnosis", "Acute upper respiratory infection");
        output.put("notes", "Patient presents with cough, congestion, low-grade fever for 3 days");
        output.put("followUpNeeded", true);
        output.put("durationMinutes", 15);
        result.setOutputData(output);
        return result;
    }
}
