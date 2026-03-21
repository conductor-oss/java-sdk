package mentalhealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class AssessWorker implements Worker {

    @Override
    public String getTaskDefName() { return "mh_assess"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [assess] Clinical assessment performed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("diagnosis", "Major Depressive Disorder, single episode, moderate");
        output.put("icdCode", "F32.1");
        output.put("severity", "moderate");
        output.put("phq9Score", 14);
        output.put("gad7Score", 8);
        result.setOutputData(output);
        return result;
    }
}
