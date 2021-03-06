package nl.tno.fpai.demo.scenario.impl;

import java.util.Locale;
import java.util.Set;

import nl.tno.fpai.demo.scenario.ScenarioManager;

import org.flexiblepower.ui.Widget;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(property = "widget.ranking=999999")
public class ScenarioWidget implements Widget {

    private ScenarioManagerImpl scenarioManager;

    @Reference
    public void setScenarioManager(ScenarioManager scenarioManager) {
        this.scenarioManager = (ScenarioManagerImpl) scenarioManager;
    }

    @Override
    public String getTitle(Locale locale) {
        return "Demonstration Scenario";
    }

    public Set<String> getNames() {
        return scenarioManager.getScenarios();
    }

    public void startScenario(String name) {
        scenarioManager.startScenario(name);
    }

    public String getStatus() {
        return scenarioManager.getStatus();
    }
}
