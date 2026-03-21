package publichealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SurveillanceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "phw_surveillance";
    }

    @Override
    public TaskResult execute(Task task) {
        String disease = (String) task.getInputData().get("disease");
        String region = (String) task.getInputData().get("region");
        System.out.printf("  [surveillance] Monitoring %s in %s%n", disease, region);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("baseline", 15);
        result.getOutputData().put("reportPeriod", "weekly");
        return result;
    }
}
