package clinicaldecision.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApplyGuidelinesWorker implements Worker {

    @Override
    public String getTaskDefName() { return "cds_apply_guidelines"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [guidelines] Applying ACC/AHA guidelines for " + task.getInputData().get("condition"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> guidelines = new LinkedHashMap<>();
        guidelines.put("guideline", "ACC/AHA 2019 CVD Prevention");
        guidelines.put("statinIndicated", true);
        guidelines.put("aspirinIndicated", true);
        guidelines.put("bpTarget", "< 130/80");
        guidelines.put("ldlTarget", "< 70");
        output.put("guidelineResults", guidelines);
        result.setOutputData(output);
        return result;
    }
}
