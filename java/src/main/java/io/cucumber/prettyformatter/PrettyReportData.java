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
import io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature;
import io.cucumber.query.Lineage;
import io.cucumber.query.Query;
import io.cucumber.query.Repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_FEATURE_LINE;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.INCLUDE_RULE_LINE;
import static io.cucumber.prettyformatter.MessagesToPrettyWriter.PrettyFeature.USE_STATUS_ICON;
import static io.cucumber.query.Repository.RepositoryFeature.INCLUDE_GHERKIN_DOCUMENTS;
import static io.cucumber.query.Repository.RepositoryFeature.INCLUDE_STEP_DEFINITIONS;

final class PrettyReportData {

    // Visually the icon is assumed to have length 1
    static final int VISUAL_STATUS_ICON_LENGTH = 1;
    private static final int AFTER_SCENARIO_ATTACHMENT_INDENT = 6;
    private static final int AFTER_STEP_STACKTRACE_INDENT = 4;
    private static final int AFTER_STEP_ARGUMENT_INDENT = 2;
    private static final int STEP_INDENT = 2;
    private static final int ONE_SPACE_LENGTH = 1;

    private final Repository repository = Repository.builder()
            .feature(INCLUDE_GHERKIN_DOCUMENTS, true)
            .feature(INCLUDE_STEP_DEFINITIONS, true)
            .build();
    private final Query query = new Query(repository);
    private final Map<String, Integer> commentStartIndexByTestCaseStartedId = new HashMap<>();
    private final Map<String, Integer> scenarioIndentByTestCaseStartedId = new HashMap<>();
    private final Set<Object> printedFeaturesAndRules = new HashSet<>();
    private final int afterFeatureIndent;
    private final int afterRuleIndent;
    private final int iconLength;

    PrettyReportData(Set<PrettyFeature> features) {
        afterFeatureIndent = calculateAfterFeatureIndent(features);
        afterRuleIndent = calculateAfterRuleIndent(features);
        iconLength = calculateIconLength(features);
    }

    private static int calculateAfterRuleIndent(Set<PrettyFeature> features) {
        int indent = 0;
        if (features.contains(INCLUDE_FEATURE_LINE)) {
            indent += 2;
        }
        if (features.contains(INCLUDE_RULE_LINE)) {
            indent += 2;
        }
        return indent;
    }

    private static int calculateAfterFeatureIndent(Set<PrettyFeature> features) {
        int indent = 0;
        if (features.contains(INCLUDE_FEATURE_LINE)) {
            indent += 2;
        }
        return indent;
    }

    private static int calculateScenarioLineLength(int scenarioIndent, Pickle pickle, Scenario scenario) {
        String pickleName = pickle.getName();
        String pickleKeyword = scenario.getKeyword();
        // The ": " between keyword and name adds 2
        return scenarioIndent + pickleKeyword.length() + 2 + pickleName.length();
    }

    private static int calculateIconLength(Set<PrettyFeature> features) {
        // The icon plus a space to create separation between the step
        return features.contains(USE_STATUS_ICON) ? VISUAL_STATUS_ICON_LENGTH + ONE_SPACE_LENGTH : 0;
    }

    private int calculateStepLineLength(int scenarioIndent, Step step, PickleStep pickleStep) {
        String keyword = step.getKeyword();
        String text = pickleStep.getText();
        // The step indentation adds 2
        return scenarioIndent + STEP_INDENT + iconLength + keyword.length() + text.length();
    }

    private int calculateScenarioIndent(Lineage lineage) {
        if (lineage.rule().isPresent()) {
            return afterRuleIndent;
        }
        if (lineage.feature().isPresent()) {
            return afterFeatureIndent;
        }
        return 0;
    }

    void update(Envelope envelope) {
        repository.update(envelope);
        envelope.getTestCaseStarted().ifPresent(this::preCalculateLocationIndent);
    }

    private void preCalculateLocationIndent(TestCaseStarted event) {
        query.findLineageBy(event)
                .ifPresent(lineage ->
                        lineage.scenario().ifPresent(scenario ->
                                query.findPickleBy(event).ifPresent(pickle -> {
                                    int scenarioIndent = calculateScenarioIndent(lineage);
                                    int scenarioLineLength = calculateScenarioLineLength(scenarioIndent, pickle, scenario);
                                    int longestLine = pickle.getSteps().stream()
                                            .mapToInt(pickleStep -> preCalculatePickleStepLineLength(scenarioIndent, pickleStep))
                                            .reduce(scenarioLineLength, Math::max);

                                    scenarioIndentByTestCaseStartedId.put(event.getId(), scenarioIndent);
                                    // Adds Space between step and comment start
                                    commentStartIndexByTestCaseStartedId.put(event.getId(), longestLine + ONE_SPACE_LENGTH);
                                })));
    }

    private Integer preCalculatePickleStepLineLength(int indent, PickleStep pickleStep) {
        return query.findStepBy(pickleStep)
                .map(step -> calculateStepLineLength(indent, step, pickleStep))
                .orElse(0);
    }

    int getAfterFeatureIndent() {
        return afterFeatureIndent;
    }

    int getAttachmentIndentBy(Attachment attachment) {
        return attachment.getTestCaseStartedId()
                .map(this::getScenarioIndentBy)
                .orElse(0) + AFTER_SCENARIO_ATTACHMENT_INDENT + iconLength;
    }

    int getScenarioIndentBy(TestCaseStarted testCaseStarted) {
        return getScenarioIndentBy(testCaseStarted.getId());
    }

    int getStepIndentBy(TestStepFinished testStepFinished) {
        return getScenarioIndentBy(testStepFinished.getTestCaseStartedId()) + STEP_INDENT;
    }

    private Integer getScenarioIndentBy(String testCaseStartedId) {
        return scenarioIndentByTestCaseStartedId.getOrDefault(testCaseStartedId, 0);
    }

    int getStackTraceIndentBy(TestStepFinished testStepFinished) {
        return getStepIndentBy(testStepFinished) + iconLength + AFTER_STEP_STACKTRACE_INDENT;
    }

    int getArgumentIndentBy(TestStepFinished testStepFinished) {
        return getStepIndentBy(testStepFinished) + iconLength + AFTER_STEP_ARGUMENT_INDENT;
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
        return query.findUnambiguousStepDefinitionBy(testStep)
                .map(StepDefinition::getSourceReference);
    }

    Optional<Pickle> findPickleBy(TestCaseStarted testCaseStarted) {
        return query.findPickleBy(testCaseStarted);
    }

    Optional<Long> findLineOf(Pickle pickle) {
        return query.findLocationOf(pickle).map(Location::getLine);
    }

    Optional<Lineage> findLineageBy(TestCaseStarted event) {
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
