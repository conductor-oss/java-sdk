package medicalimaging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class ProcessImageWorker implements Worker {

    @Override
    public String getTaskDefName() { return "img_process"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [process] Enhancing image " + task.getInputData().get("imageId")
                + " — noise reduction + contrast");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("processedImageId", "IMG-PROC-88201");
        output.put("enhancements", List.of("denoise", "contrast", "edge_detection"));
        result.setOutputData(output);
        return result;
    }
}
