package dischargeplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Provides patient education for discharge.
 * Input: patientId, diagnosis, medications
 * Output: educated, topics
 */
public class EducateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dsc_educate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String diagnosis = (String) task.getInputData().get("diagnosis");
        if (diagnosis == null) diagnosis = "unspecified";

        Object medsObj = task.getInputData().get("medications");
        List<?> meds = medsObj instanceof List ? (List<?>) medsObj : List.of();

        System.out.println("  [educate] Patient education: " + diagnosis + ", " + meds.size() + " medications");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("educated", true);
        result.getOutputData().put("topics", List.of("medications", "warning_signs", "activity_restrictions", "diet"));
        return result;
    }
}
