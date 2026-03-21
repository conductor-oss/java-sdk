package publishsubscribe.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PbsConfirmWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pbs_confirm";
    }

    @Override
    public TaskResult execute(Task task) {
        int delivered = 0;
        for (String key : java.util.List.of("sub1Status","sub2Status","sub3Status")) {
            Object v = task.getInputData().get(key);
            if (Boolean.TRUE.equals(v) || "true".equals(String.valueOf(v))) delivered++;
        }
        System.out.println("  [confirm] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("subscribersNotified", delivered);
        result.getOutputData().put("allDelivered", delivered == 3);
        return result;
    }
}