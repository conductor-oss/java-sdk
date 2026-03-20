package genomicspipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class SequenceWorker implements Worker {

    @Override
    public String getTaskDefName() { return "gen_sequence"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sequence] Sequencing sample " + task.getInputData().get("sampleId")
                + ", panel: " + task.getInputData().get("panelType"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("totalReads", 45000000);
        output.put("meanQuality", 35.2);
        output.put("referenceGenome", "GRCh38");
        output.put("coverageDepth", "100x");
        result.setOutputData(output);
        return result;
    }
}
