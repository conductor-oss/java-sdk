package genomicspipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class AlignWorker implements Worker {

    @Override
    public String getTaskDefName() { return "gen_align"; }

    @Override
    public TaskResult execute(Task task) {
        long reads = 0;
        try { reads = Long.parseLong(String.valueOf(task.getInputData().getOrDefault("reads", "0"))); }
        catch (NumberFormatException ignored) {}
        System.out.println("  [align] Aligning " + (reads / 1_000_000) + "M reads to " + task.getInputData().get("referenceGenome"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("alignmentFile", "aligned_reads.bam");
        output.put("mappedReads", Math.round(reads * 0.98));
        output.put("mappingRate", 98.2);
        result.setOutputData(output);
        return result;
    }
}
