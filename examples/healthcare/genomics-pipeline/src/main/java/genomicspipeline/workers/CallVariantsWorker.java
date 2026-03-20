package genomicspipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class CallVariantsWorker implements Worker {

    @Override
    public String getTaskDefName() { return "gen_call_variants"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [variants] Calling variants from " + task.getInputData().get("alignmentFile"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        List<Map<String, Object>> variants = new ArrayList<>();
        variants.add(Map.of("gene", "BRCA1", "position", "chr17:41245466", "type", "SNV", "zygosity", "heterozygous"));
        variants.add(Map.of("gene", "TP53", "position", "chr17:7576738", "type", "SNV", "zygosity", "heterozygous"));
        variants.add(Map.of("gene", "EGFR", "position", "chr7:55259515", "type", "SNV", "zygosity", "homozygous"));
        output.put("variants", variants);
        output.put("totalVariants", 3);
        output.put("snvCount", 3);
        output.put("indelCount", 0);
        result.setOutputData(output);
        return result;
    }
}
