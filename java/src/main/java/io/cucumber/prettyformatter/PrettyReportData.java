package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Attachment;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.Pickle;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleTag;
import io.cucumber.messages.types.Rule;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class PrettyReportData {

    final Query query = new Query();
    private final Map<String, Integer> commentStartIndexByTestCaseStartedId = new HashMap<>();
    private final Map<String, String> scenarioIndentByTestCaseStartedId = new HashMap<>();
    private final Map<String, StepDefinition> stepDefinitionsById = new HashMap<>();
    private final boolean includeFeatureAndRules;
    private final Set<Object> printedFeaturesAndRules = new HashSet<>();

    PrettyReportData(boolean includeFeatureAndRules) {
        this.includeFeatureAndRules = includeFeatureAndRules;
    }

    private static int calculateStepLineLength(String scenarioIndent, Step step, PickleStep pickleStep) {
        String keyword = step.getKeyword();
        String text = pickleStep.getText();
        return scenarioIndent.length() + 2 + keyword.length() + text.length();
    }

    private static int calculateScenarioLineLength(String scenarioIndent, Pickle pickle, Scenario scenario) {
        String pickleName = pickle.getName();
        String pickleKeyword = scenario.getKeyword();
        // The ": " between keyword and name adds 2
        return scenarioIndent.length() + pickleName.length() + pickleKeyword.length() + 2;
    }

    private String calculateScenarioIndent(Lineage lineage) {
        if (includeFeatureAndRules) {
            return lineage.rule().isPresent() ? "    " : lineage.feature().isPresent() ? "  " : "";
        }
        return "";
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
                .ifPresent(lineage ->
                        lineage.scenario().ifPresent(scenario ->
                                query.findPickleBy(event).ifPresent(pickle -> {
                                    String scenarioIndent = calculateScenarioIndent(lineage);
                                    int scenarioLineLength = calculateScenarioLineLength(scenarioIndent, pickle, scenario);
                                    int longestLine = pickle.getSteps().stream()
                                            .mapToInt(pickleStep -> calculatePickleStepLineLength(scenarioIndent, pickleStep))
                                            .reduce(scenarioLineLength, Math::max);

                                    scenarioIndentByTestCaseStartedId.put(event.getId(), scenarioIndent);
                                    commentStartIndexByTestCaseStartedId.put(event.getId(), longestLine + 1);
                                })));
    }

    private Integer calculatePickleStepLineLength(String indent, PickleStep pickleStep) {
        return query.findStepBy(pickleStep)
                .map(step -> calculateStepLineLength(indent, step, pickleStep))
                .orElse(0);
    }

    public String getAttachmentIndentBy(Attachment attachment) {
        return attachment.getTestCaseStartedId()
                .map(s -> scenarioIndentByTestCaseStartedId.getOrDefault(s, "") + "    ")
                .orElse("  ");
    }

    String getScenarioIndentBy(TestCaseStarted testCaseStarted) {
        return scenarioIndentByTestCaseStartedId.getOrDefault(testCaseStarted.getId(), "");
    }

    String getStepIndentBy(TestStepFinished testStepFinished) {
        return scenarioIndentByTestCaseStartedId.getOrDefault(testStepFinished.getTestCaseStartedId(), "") + "  ";
    }

    String getStackTraceIndentBy(TestStepFinished testStepFinished) {
        return getStepIndentBy(testStepFinished) + "    ";
    }

    String getArgumentIndentBy(TestStepFinished testStepFinished) {
        return getStepIndentBy(testStepFinished) + "  ";
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

    Optional<Lineage> findLineageBy(TestCaseStarted event, MessagesToPrettyWriter messagesToPrettyWriter) {
        return query.findLineageBy(event);
    }

    void ifNotSeenBefore(Feature feature, Runnable print) {
        if (printedFeaturesAndRules.add(feature)) {
            print.run();
        }
    }

    void ifNotSeenBefore(Rule rule, Runnable print) {
        if (printedFeaturesAndRules.add(rule)) {
            print.run();
        }
    }
}
