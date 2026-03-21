package costoptimization;

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
        assertEquals("co_collect_billing", new costoptimization.workers.CollectBillingWorker().getTaskDefName());
        assertEquals("co_analyze_usage", new costoptimization.workers.AnalyzeUsageWorker().getTaskDefName());
        assertEquals("co_recommend", new costoptimization.workers.RecommendWorker().getTaskDefName());
        assertEquals("co_apply_savings", new costoptimization.workers.ApplySavingsWorker().getTaskDefName());
    }
}
