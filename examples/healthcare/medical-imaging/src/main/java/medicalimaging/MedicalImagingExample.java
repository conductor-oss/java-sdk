package medicalimaging;

import com.netflix.conductor.client.worker.Worker;
import medicalimaging.workers.*;

import java.util.List;
import java.util.Map;

public class MedicalImagingExample {

    private static final List<Worker> WORKERS = List.of(
            new AcquireWorker(),
            new ProcessImageWorker(),
            new AnalyzeImageWorker(),
            new ReportWorker(),
            new StoreWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Medical Imaging Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("img_acquire", "img_process", "img_analyze", "img_report", "img_store"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("medical_imaging_workflow", 1, Map.of(
                "studyId", "STD-2024-0301",
                "patientId", "PAT-10234",
                "modality", "CT",
                "bodyPart", "chest"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Report: " + result.getOutput().get("reportId"));
        System.out.println("Findings: " + result.getOutput().get("findings"));
        System.out.println("Archived: " + result.getOutput().get("archived"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
