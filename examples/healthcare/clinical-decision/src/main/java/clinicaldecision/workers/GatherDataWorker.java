package clinicaldecision.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class GatherDataWorker implements Worker {

    @Override
    public String getTaskDefName() { return "cds_gather_data"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [gather] Collecting clinical data for " + task.getInputData().get("patientId")
                + ", condition: " + task.getInputData().get("condition"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("age", 62);
        data.put("gender", "male");
        data.put("bmi", 29.5);
        data.put("smoker", true);
        data.put("diabetes", true);
        data.put("hypertension", true);
        data.put("ldl", 145);
        data.put("hdl", 38);
        data.put("systolicBP", 148);
        data.put("familyHistory", true);
        output.put("clinicalData", data);
        result.setOutputData(output);
        return result;
    }
}
