package telemedicine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConnectWorker implements Worker {

    @Override
    public String getTaskDefName() { return "tlm_connect"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [connect] Video session established for visit " + task.getInputData().get("visitId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("connectionId", "CONN-TLM-441");
        output.put("quality", "HD");
        output.put("latency", "45ms");
        result.setOutputData(output);
        return result;
    }
}
