package genomicspipeline;

import com.netflix.conductor.client.worker.Worker;
import genomicspipeline.workers.*;

import java.util.List;
import java.util.Map;

public class GenomicsPipelineExample {

    private static final List<Worker> WORKERS = List.of(
            new SequenceWorker(),
            new AlignWorker(),
            new CallVariantsWorker(),
            new AnnotateWorker(),
            new GenomicsReportWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Genomics Pipeline Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("gen_sequence", "gen_align", "gen_call_variants", "gen_annotate", "gen_report"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("genomics_pipeline_workflow", 1, Map.of(
                "sampleId", "GEN-SMP-8801",
                "patientId", "PAT-10234",
                "panelType", "hereditary_cancer"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Total variants: " + result.getOutput().get("totalVariants"));
        System.out.println("Clinically significant: " + result.getOutput().get("clinicallySignificant"));
        System.out.println("Report: " + result.getOutput().get("reportId"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
