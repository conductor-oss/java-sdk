package cryptocurrencytrading;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import cryptocurrencytrading.workers.*;
import java.util.List;
import java.util.Map;
public class CryptocurrencyTradingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 510: Cryptocurrency Trading ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cry_monitor_market","cry_analyze_signals","cry_execute_buy","cry_execute_sell","cry_execute_hold","cry_confirm_action"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new MonitorMarketWorker(), new AnalyzeSignalsWorker(), new ExecuteBuyWorker(), new ExecuteSellWorker(), new ExecuteHoldWorker(), new ConfirmActionWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cryptocurrency_trading_workflow", 1, Map.of("pair","BTC/USD","portfolioId","PORT-CRYPTO-01","strategy","momentum"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
