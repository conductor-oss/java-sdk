package druginteraction.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class ListMedicationsWorker implements Worker {

    @Override
    public String getTaskDefName() { return "drg_list_medications"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [list] Retrieving medication list for patient " + task.getInputData().get("patientId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        List<Map<String, Object>> meds = new ArrayList<>();
        meds.add(Map.of("name", "Warfarin", "dosage", "5mg", "frequency", "daily"));
        meds.add(Map.of("name", "Lisinopril", "dosage", "10mg", "frequency", "daily"));
        meds.add(Map.of("name", "Metformin", "dosage", "500mg", "frequency", "twice daily"));
        output.put("medications", meds);
        result.setOutputData(output);
        return result;
    }
}
