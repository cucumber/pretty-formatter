package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;

import java.util.AbstractMap;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import static io.cucumber.messages.types.TestStepResultStatus.AMBIGUOUS;
import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static io.cucumber.messages.types.TestStepResultStatus.PASSED;
import static io.cucumber.messages.types.TestStepResultStatus.PENDING;
import static io.cucumber.messages.types.TestStepResultStatus.SKIPPED;
import static io.cucumber.messages.types.TestStepResultStatus.UNDEFINED;
import static io.cucumber.prettyformatter.Ansi.Attributes.BOLD;
import static io.cucumber.prettyformatter.Ansi.Attributes.BOLD_OFF;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_BLUE;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_BRIGHT_BLACK;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_CYAN;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_GREEN;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_RED;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_YELLOW;
import static io.cucumber.prettyformatter.Ansi.Attributes.RESET;
import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION;
import static io.cucumber.prettyformatter.Theme.Element.RULE_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.STEP;
import static io.cucumber.prettyformatter.Theme.Element.STEP_ARGUMENT;
import static io.cucumber.prettyformatter.Theme.Element.STEP_KEYWORD;
import static java.util.Objects.requireNonNull;

/**
 * An ANSI theme for the pretty formatter.
 * <p>
 * The theme consists of a collection of stylable {@linkplain Element elements}.
 * For each element an {@link Ansi ansi style} is declared. The ansi style
 * consists of two escape sequences that are wrapped around the text of stylable
 * element.
 */
public final class Theme {

    private final Map<Element, Entry<Ansi, Ansi>> styleByElement;
    private final Map<Element, Map<TestStepResultStatus, Entry<Ansi, Ansi>>> styleByStatusByElement;

    private Theme(
            Map<Element, Entry<Ansi, Ansi>> styleByElement,
            Map<Element, Map<TestStepResultStatus, Entry<Ansi, Ansi>>> styleByStatusByElement
    ) {
        this.styleByElement = requireNonNull(styleByElement);
        this.styleByStatusByElement = requireNonNull(styleByStatusByElement);
    }

    /**
     * The default Cucumber theme.
     */
    public static Theme cucumber() {
        return Theme.builder()
                .style(ATTACHMENT, Ansi.with(FOREGROUND_BLUE), Ansi.with(RESET))
                .style(FEATURE_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(LOCATION, Ansi.with(FOREGROUND_BRIGHT_BLACK), Ansi.with(RESET))
                .style(RULE_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(SCENARIO_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(STEP, AMBIGUOUS, Ansi.with(FOREGROUND_RED), Ansi.with(RESET))
                .style(STEP, FAILED, Ansi.with(FOREGROUND_RED), Ansi.with(RESET))
                .style(STEP, PASSED, Ansi.with(FOREGROUND_GREEN), Ansi.with(RESET))
                .style(STEP, PENDING, Ansi.with(FOREGROUND_YELLOW), Ansi.with(RESET))
                .style(STEP, SKIPPED, Ansi.with(FOREGROUND_CYAN), Ansi.with(RESET))
                .style(STEP, UNDEFINED, Ansi.with(FOREGROUND_YELLOW), Ansi.with(RESET))
                .style(STEP_ARGUMENT, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(STEP_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .build();
    }

    /**
     * Empty theme that does not apply any styling to the output.
     */
    public static Theme none() {
        return Theme.builder().build();
    }

    /**
     * Creates a new builder to construct a theme.
     */
    public static Builder builder() {
        return new Builder();
    }

    String style(Element element, String text) {
        Entry<Ansi, Ansi> ansiStyle = findAnsiBy(element);
        return ansiStyle == null ? text : ansiStyle.getKey() + text + ansiStyle.getValue();
    }

    String beginStyle(Element element) {
        Entry<Ansi, Ansi> style = findAnsiBy(element);
        return style == null ? "" : style.getKey().toString();
    }

    String beginStyle(Element element, TestStepResultStatus status) {
        Entry<Ansi, Ansi> style = findAnsiBy(element, status);
        return style == null ? "" : style.getKey().toString();
    }

    String endStyle(Element element) {
        Entry<Ansi, Ansi> style = findAnsiBy(element);
        return style == null ? "" : style.getValue().toString();
    }

    String endStyle(Element element, TestStepResultStatus status) {
        Entry<Ansi, Ansi> style = findAnsiBy(element, status);
        return style == null ? "" : style.getValue().toString();
    }

    private Entry<Ansi, Ansi> findAnsiBy(Element element) {
        return styleByElement.get(element);
    }

    private Entry<Ansi, Ansi> findAnsiBy(Element element, TestStepResultStatus status) {
        Map<TestStepResultStatus, Entry<Ansi, Ansi>> styleByStatus = styleByStatusByElement.get(element);
        return styleByStatus == null ? null : styleByStatus.get(status);
    }

    /**
     * All style-able elements in a theme.
     */
    public enum Element {
        /**
         * The output from {@code scenario.log} and {@code scenario.attach}.
         */
        ATTACHMENT,
        /**
         * The data table, an optional argument for a step.
         */
        DATA_TABLE,
        /**
         * The data table borders. I.e. the {code |} characters.
         * <p>
         * Styles applied to {@link #DATA_TABLE} are also applied to this element.
         */
        DATA_TABLE_BORDER,
        /**
         * The data table contents. I.e. the individual cell.
         * <p>
         * Styles applied to {@link #DATA_TABLE} are also applied to this element.
         */
        DATA_TABLE_CONTENT,
        /**
         * The doc string, an optional argument for a step.
         */
        DOC_STRING,
        /**
         * The doc string contents.
         * <p>
         * Styles applied to {@link #DOC_STRING} are also applied to this element.
         */
        DOC_STRING_CONTENT,
        /**
         * The doc string media type. E.g. {@code application/json} .
         * <p>
         * Styles applied to {@link #DOC_STRING} are also applied to this element.
         */
        DOC_STRING_MEDIA_TYPE,
        /**
         * The doc string delimiter. I.e. {@code """"}.
         * <p>
         * Styles applied to {@link #DOC_STRING} are also applied to this element.
         */
        DOC_STRING_DELIMITER,
        /**
         * The feature line.
         */
        FEATURE,
        /**
         * The feature keyword.
         * <p>
         * Styles applied to {@link #FEATURE} are also applied to this element.
         */
        FEATURE_KEYWORD,
        /**
         * The feature name.
         * <p>
         * Styles applied to {@link #FEATURE} are also applied to this element.
         */
        FEATURE_NAME,
        /**
         * The location comment. E.g. {@code # samples/undefined/undefined.feature:10}.
         * <p>
         * Styles applied to {@link #FEATURE} are also applied to this element.
         */
        LOCATION,
        /**
         * The rule line.
         */
        RULE,
        /**
         * The rule keyword.
         * <p>
         * Styles applied to {@link #RULE} are also applied to this element.
         */
        RULE_KEYWORD,
        /**
         * The rule name.
         * <p>
         * Styles applied to {@link #RULE} are also applied to this element.
         */
        RULE_NAME,
        /**
         * The scenario line.
         */
        SCENARIO,
        /**
         * The scenario keyword.
         * <p>
         * Styles applied to {@link #SCENARIO} are also applied to this element.
         */
        SCENARIO_KEYWORD,
        /**
         * The scenario name.
         * <p>
         * Styles applied to {@link #SCENARIO} are also applied to this element.
         */
        SCENARIO_NAME,
        /**
         * The step line.
         */
        STEP,
        /**
         * A matched argument in a step.
         * <p>
         * Styles applied to {@link #STEP} are also applied to this element.
         */
        STEP_ARGUMENT,
        /**
         * The step keyword.
         * <p>
         * Styles applied to {@link #STEP} are also applied to this element.
         */
        STEP_KEYWORD,
        /**
         * The step text.
         * <p>
         * Styles applied to {@link #STEP} are also applied to this element.
         */
        STEP_TEXT,
        /**
         * The tag line.
         */
        TAG
    }

    public static class Builder {
        private final Map<Element, Entry<Ansi, Ansi>> styleByElement = new EnumMap<>(Element.class);
        private final Map<Element, Map<TestStepResultStatus, Entry<Ansi, Ansi>>> styleByStatusByElement = new EnumMap<>(Element.class);

        private Builder() {

        }

        /**
         * Adds a style and reset style for an element.
         *
         * @param element    the element to style
         * @param style      the ansi style to apply
         * @param resetStyle the ansi style to reset the applied styling
         */
        public Builder style(Element element, Ansi style, Ansi resetStyle) {
            requireNonNull(element);
            requireNonNull(style);
            requireNonNull(resetStyle);

            styleByElement.put(element, new AbstractMap.SimpleEntry<>(style, resetStyle));
            return this;
        }

        /**
         * Adds a style and reset style for an element.
         *
         * @param element    the element to style
         * @param status     the status of the element to style
         * @param style      the ansi style to apply
         * @param resetStyle the ansi style to reset the applied styling
         */
        public Builder style(Element element, TestStepResultStatus status, Ansi style, Ansi resetStyle) {
            requireNonNull(element);
            requireNonNull(status);
            requireNonNull(style);
            requireNonNull(resetStyle);

            styleByStatusByElement.computeIfAbsent(element, e1 -> new EnumMap<>(TestStepResultStatus.class))
                    .put(status, new AbstractMap.SimpleEntry<>(style, resetStyle));
            return this;
        }

        public Theme build() {
            return new Theme(styleByElement, styleByStatusByElement);
        }

    }

}
