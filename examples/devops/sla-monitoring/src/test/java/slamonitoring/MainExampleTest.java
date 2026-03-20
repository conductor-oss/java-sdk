package slamonitoring;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainExampleTest {

    @Test
    void workflowJsonIsLoadable() {
        var is = getClass().getClassLoader().getResourceAsStream("workflow.json");
        assertNotNull(is, "workflow.json should be loadable from resources");
    }

    @Test
    void workerInstantiation() {
        assertEquals("sla_collect_slis", new slamonitoring.workers.CollectSlisWorker().getTaskDefName());
        assertEquals("sla_calculate_budget", new slamonitoring.workers.CalculateBudgetWorker().getTaskDefName());
        assertEquals("sla_evaluate_compliance", new slamonitoring.workers.EvaluateComplianceWorker().getTaskDefName());
        assertEquals("sla_report", new slamonitoring.workers.ReportWorker().getTaskDefName());
    }
}
