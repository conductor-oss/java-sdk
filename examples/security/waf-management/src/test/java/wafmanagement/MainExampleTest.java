package wafmanagement;

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
        assertEquals("waf_analyze_traffic", new wafmanagement.workers.AnalyzeTrafficWorker().getTaskDefName());
        assertEquals("waf_update_rules", new wafmanagement.workers.UpdateRulesWorker().getTaskDefName());
        assertEquals("waf_deploy_rules", new wafmanagement.workers.DeployRulesWorker().getTaskDefName());
        assertEquals("waf_verify_protection", new wafmanagement.workers.VerifyProtectionWorker().getTaskDefName());
    }
}
