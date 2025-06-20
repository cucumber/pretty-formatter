package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.StepDefinition;
import io.cucumber.query.Lineage;
import io.cucumber.query.Query;

import java.util.HashMap;
import java.util.Map;

class PrettyReportData {

    final Query query = new Query();
    final Map<String, Integer> commentStartIndexByTestCaseId = new HashMap<>();
    final Map<String, StepDefinition> stepDefinitionsById = new HashMap<>();

    public void collect(Envelope envelope) {
        query.update(envelope);
        envelope.getStepDefinition().ifPresent(this::updateStepDefinitionsById);
        envelope.getTestCaseStarted().ifPresent(this::preCalculateLocationIndent);
    }

    private void updateStepDefinitionsById(StepDefinition stepDefinition) {
        stepDefinitionsById.put(stepDefinition.getId(), stepDefinition);
    }

    private void preCalculateLocationIndent(io.cucumber.messages.types.TestCaseStarted event) {
        query.findLineageBy(event)
                .flatMap(Lineage::scenario)
                .ifPresent(scenario -> {
                    query.findPickleBy(event).ifPresent(pickle -> {
                        int scenarioLineLength = calculateScenarioLineLength(pickle, scenario);
                        int longestLine = pickle.getSteps().stream()
                                .mapToInt(pickleStep -> query.findStepBy(pickleStep)
                                        .map(step -> calculateStepLineLength(step, pickleStep))
                                        .orElse(0))
                                .reduce(scenarioLineLength, Math::max);
                        commentStartIndexByTestCaseId.put(event.getTestCaseId(), longestLine + 1);
                    });
                });
    }

    private static int calculateStepLineLength(Step step, PickleStep pickleStep) {
        String keyword = step.getKeyword();
        String text = pickleStep.getText();
        return MessagesToPrettyWriter.STEP_INDENT.length() + keyword.length() + text.length();
    }

    private static int calculateScenarioLineLength(Pickle pickle, Scenario scenario) {
        String pickleName = pickle.getName();
        String pickleKeyword = scenario.getKeyword();
        // The ": " between keyword and name adds 2
        return MessagesToPrettyWriter.SCENARIO_INDENT.length() + pickleName.length() + pickleKeyword.length() + 2;
    }
}
