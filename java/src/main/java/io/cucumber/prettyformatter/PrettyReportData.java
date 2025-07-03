package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleTag;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.SourceReference;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.StepDefinition;
import io.cucumber.messages.types.TestCaseStarted;
import io.cucumber.messages.types.TestStep;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.query.Lineage;
import io.cucumber.query.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class PrettyReportData {

    private final Query query = new Query();
    private final Map<String, Integer> commentStartIndexByTestCaseStartedId = new HashMap<>();
    private final Map<String, StepDefinition> stepDefinitionsById = new HashMap<>();

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

    void collect(Envelope envelope) {
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
                .ifPresent(scenario ->
                        query.findPickleBy(event).ifPresent(pickle -> {
                            int scenarioLineLength = calculateScenarioLineLength(pickle, scenario);
                            int longestLine = pickle.getSteps().stream()
                                    .mapToInt(pickleStep -> query.findStepBy(pickleStep)
                                            .map(step -> calculateStepLineLength(step, pickleStep))
                                            .orElse(0))
                                    .reduce(scenarioLineLength, Math::max);
                            commentStartIndexByTestCaseStartedId.put(event.getId(), longestLine + 1);
                        }));
    }

    int getCommentStartAtIndexBy(TestCaseStarted testCaseStarted) {
        return getCommentStartAtIndexBy(testCaseStarted.getId());
    }

    int getCommentStartAtIndexBy(TestStepFinished testStepFinished) {
        return getCommentStartAtIndexBy(testStepFinished.getTestCaseStartedId());
    }

    private int getCommentStartAtIndexBy(String testCaseStartedId) {
        return commentStartIndexByTestCaseStartedId.getOrDefault(testCaseStartedId, 0);

    }

    Optional<List<PickleTag>> findTagsBy(TestCaseStarted testCaseStarted) {
        return query.findPickleBy(testCaseStarted)
                .map(Pickle::getTags)
                .filter(pickleTags -> !pickleTags.isEmpty());
    }

    Optional<Scenario> findScenarioBy(Pickle pickle) {
        return query.findLineageBy(pickle)
                .flatMap(Lineage::scenario);
    }

    Optional<TestStep> findTestStepBy(TestStepFinished event) {
        return query.findTestStepBy(event);
    }

    Optional<PickleStep> findPickleStepBy(TestStep testStep) {
        return query.findPickleStepBy(testStep);
    }

    Optional<Step> findStepBy(PickleStep pickleStep) {
        return query.findStepBy(pickleStep);
    }

    Optional<SourceReference> findSourceReferenceBy(TestStep testStep) {
        return testStep.getStepDefinitionIds()
                .filter(ids -> ids.size() == 1)
                .map(ids -> stepDefinitionsById.get(ids.get(0)))
                .map(StepDefinition::getSourceReference);
    }

    Optional<Pickle> findPickleBy(TestCaseStarted testCaseStarted) {
        return query.findPickleBy(testCaseStarted);
    }

    Optional<Long> findLineOf(Pickle pickle) {
        return query.findLocationOf(pickle).map(Location::getLine);
    }
}
