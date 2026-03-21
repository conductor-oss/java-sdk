package medicalimaging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class AcquireWorker implements Worker {

    @Override
    public String getTaskDefName() { return "img_acquire"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [acquire] " + task.getInputData().get("modality") + " scan of "
                + task.getInputData().get("bodyPart") + " for patient " + task.getInputData().get("patientId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("imageId", "IMG-DICOM-88201");
        output.put("format", "DICOM");
        output.put("slices", 128);
        output.put("resolution", "512x512");
        result.setOutputData(output);
        return result;
    }
}
