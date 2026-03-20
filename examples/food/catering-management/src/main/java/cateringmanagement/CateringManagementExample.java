package cateringmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import cateringmanagement.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 740: Catering Management — Inquiry, Quote, Plan Menu, Execute, Invoice
 */
public class CateringManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 740: Catering Management ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("cat_inquiry", "cat_quote", "cat_plan_menu", "cat_execute", "cat_invoice"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new InquiryWorker(), new QuoteWorker(), new PlanMenuWorker(), new ExecuteWorker(), new InvoiceWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("catering_management_740", 1,
                Map.of("clientName", "Acme Corp", "eventDate", "2026-04-15", "guestCount", 100));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
