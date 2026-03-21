package medicalimaging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class AnalyzeImageWorker implements Worker {

    @Override
    public String getTaskDefName() { return "img_analyze"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [analyze] AI analysis of " + task.getInputData().get("bodyPart") + " scan");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> findings = new LinkedHashMap<>();
        Map<String, Object> nodule = new LinkedHashMap<>();
        nodule.put("type", "nodule");
        nodule.put("size", "4mm");
        nodule.put("location", "right lower lobe");
        nodule.put("confidence", 0.87);
        findings.put("abnormalities", List.of(nodule));
        findings.put("recommendation", "Follow-up CT in 6 months");
        findings.put("severity", "low");
        output.put("findings", findings);
        output.put("modelVersion", "RadAI-v4.1");
        result.setOutputData(output);
        return result;
    }
}
