package capacityplanning;

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
        assertEquals("cp_collect_metrics", new capacityplanning.workers.CollectMetricsWorker().getTaskDefName());
        assertEquals("cp_analyze_trends", new capacityplanning.workers.AnalyzeTrendsWorker().getTaskDefName());
        assertEquals("cp_forecast", new capacityplanning.workers.ForecastWorker().getTaskDefName());
        assertEquals("cp_recommend", new capacityplanning.workers.RecommendWorker().getTaskDefName());
    }
}
