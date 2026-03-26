package mentalhealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class IntakeWorker implements Worker {

    @Override
    public String getTaskDefName() { return "mh_intake"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [intake] Mental health intake for " + task.getInputData().get("patientId")
                + ": " + task.getInputData().get("referralReason"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> intakeData = new LinkedHashMap<>();
        intakeData.put("symptoms", List.of("persistent sadness", "insomnia", "fatigue", "difficulty concentrating"));
        intakeData.put("duration", "6 weeks");
        intakeData.put("priorTreatment", false);
        intakeData.put("substanceUse", "none");
        intakeData.put("suicidalIdeation", false);
        output.put("intakeData", intakeData);
        result.setOutputData(output);
        return result;
    }
}
