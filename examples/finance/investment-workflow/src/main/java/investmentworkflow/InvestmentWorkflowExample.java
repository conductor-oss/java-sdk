package investmentworkflow;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import investmentworkflow.workers.*;
import java.util.List;
import java.util.Map;
public class InvestmentWorkflowExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 509: Investment Workflow ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ivt_research","ivt_analyze","ivt_decide","ivt_execute","ivt_monitor"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ResearchWorker(), new AnalyzeWorker(), new DecideWorker(), new ExecuteWorker(), new MonitorWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("investment_workflow", 1, Map.of("tickerSymbol","ACME","investorId","INV-9920","maxInvestment",25000));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
