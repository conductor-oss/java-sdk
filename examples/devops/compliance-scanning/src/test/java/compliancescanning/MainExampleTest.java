package compliancescanning;

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
        assertEquals("cs_discover_resources", new compliancescanning.workers.DiscoverResourcesWorker().getTaskDefName());
        assertEquals("cs_scan_policies", new compliancescanning.workers.ScanPoliciesWorker().getTaskDefName());
        assertEquals("cs_generate_report", new compliancescanning.workers.GenerateReportWorker().getTaskDefName());
        assertEquals("cs_remediate", new compliancescanning.workers.RemediateWorker().getTaskDefName());
    }
}
