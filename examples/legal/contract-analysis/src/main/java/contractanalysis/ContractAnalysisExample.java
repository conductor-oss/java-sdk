package contractanalysis;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import contractanalysis.workers.*;
import java.util.List;
import java.util.Map;
public class ContractAnalysisExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 694: Contract Analysis ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cna_parse","cna_extract","cna_analyze","cna_summarize"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new ParseWorker(),new ExtractWorker(),new AnalyzeWorker(),new SummarizeWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cna_contract_analysis",1,Map.of("contractId","CTR-400","contractType","vendor-agreement"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Risk level: "+wf.getOutput().get("riskLevel")+"\n  Summary ID: "+wf.getOutput().get("summaryId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
