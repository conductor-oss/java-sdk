package populationhealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class AggregateDataWorker implements Worker {

    @Override
    public String getTaskDefName() { return "pop_aggregate_data"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [aggregate] Aggregating data for cohort " + task.getInputData().get("cohortId")
                + ": " + task.getInputData().get("condition"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalPatients", 2450);
        data.put("avgAge", 58);
        data.put("controlledPercent", 62);
        data.put("avgHba1c", 7.8);
        output.put("populationData", data);
        output.put("totalPatients", 2450);
        result.setOutputData(output);
        return result;
    }
}
