package patientintake.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Registers a patient and captures vitals. Real vitals generation within normal ranges.
 */
public class RegisterWorker implements Worker {
    private static final Random RNG = new Random();

    @Override public String getTaskDefName() { return "pit_register"; }

    @Override public TaskResult execute(Task task) {
        String patientId = (String) task.getInputData().get("patientId");
        String name = (String) task.getInputData().get("name");
        if (patientId == null) patientId = "UNKNOWN";
        if (name == null) name = "UNKNOWN";

        // Generate realistic vitals
        int systolic = 110 + RNG.nextInt(40);  // 110-150
        int diastolic = 65 + RNG.nextInt(30);   // 65-95
        int heartRate = 60 + RNG.nextInt(40);    // 60-100
        double temp = 97.5 + RNG.nextDouble() * 2.5; // 97.5-100.0
        temp = Math.round(temp * 10.0) / 10.0;
        int spo2 = 95 + RNG.nextInt(5);          // 95-99

        Map<String, Object> vitals = new LinkedHashMap<>();
        vitals.put("bp", systolic + "/" + diastolic);
        vitals.put("hr", heartRate);
        vitals.put("temp", temp);
        vitals.put("spo2", spo2);

        String mrn = "MRN-" + patientId;

        System.out.println("  [register] Patient " + name + " (" + patientId + ") - BP: " + systolic + "/" + diastolic
                + ", HR: " + heartRate + ", Temp: " + temp + ", SpO2: " + spo2);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("registeredAt", Instant.now().toString());
        result.getOutputData().put("vitals", vitals);
        result.getOutputData().put("mrn", mrn);
        return result;
    }
}
