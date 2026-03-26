package ehrintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class QueryPatientWorker implements Worker {

    @Override
    public String getTaskDefName() { return "ehr_query_patient"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [query] Fetching records for patient " + task.getInputData().get("patientId")
                + " from " + task.getInputData().get("sourceSystem"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(Map.of("source", "hospital_ehr", "name", "Sarah Johnson", "dob", "1985-06-15"));
        records.add(Map.of("source", "clinic_emr", "name", "Sarah J. Johnson", "dob", "1985-06-15"));
        records.add(Map.of("source", "pharmacy_sys", "name", "Sarah Johnson", "dob", "1985-06-15"));
        output.put("records", records);
        result.setOutputData(output);
        return result;
    }
}
