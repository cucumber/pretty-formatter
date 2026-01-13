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
import static io.cucumber.prettyformatter.Ansi.Attributes.*;
import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION;
import static io.cucumber.prettyformatter.Theme.Element.PROGRESS_ICON;
import static io.cucumber.prettyformatter.Theme.Element.RULE_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.STATUS_ICON;
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
    private final Map<TestStepResultStatus, String> statusIconByStatus;
    private final Map<TestStepResultStatus, String> progressIconByStatus;
    private final String bulletPointIcon;

    private Theme(
            Map<Element, Entry<Ansi, Ansi>> styleByElement,
            Map<Element, Map<TestStepResultStatus, Entry<Ansi, Ansi>>> styleByStatusByElement,
            Map<TestStepResultStatus, String> statusIconByStatus,
            Map<TestStepResultStatus, String> progressIconByStatus, String bulletPointIcon
    ) {
        this.styleByElement = requireNonNull(styleByElement);
        this.styleByStatusByElement = requireNonNull(styleByStatusByElement);
        this.statusIconByStatus = statusIconByStatus;
        this.progressIconByStatus = progressIconByStatus;
        this.bulletPointIcon = bulletPointIcon;
    }

    /**
     * The default Cucumber theme.
     */
    public static Theme cucumber() {
        return Theme.builder()
                .style(ATTACHMENT, Ansi.with(FOREGROUND_BLUE), Ansi.with(FOREGROUND_DEFAULT))
                .style(FEATURE_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(LOCATION, Ansi.with(FOREGROUND_BRIGHT_BLACK), Ansi.with(FOREGROUND_DEFAULT))
                .style(RULE_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(SCENARIO_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(STEP, AMBIGUOUS, Ansi.with(FOREGROUND_RED), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP, FAILED, Ansi.with(FOREGROUND_RED), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP, PASSED, Ansi.with(FOREGROUND_GREEN), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP, PENDING, Ansi.with(FOREGROUND_YELLOW), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP, SKIPPED, Ansi.with(FOREGROUND_CYAN), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP, UNDEFINED, Ansi.with(FOREGROUND_YELLOW), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP_ARGUMENT, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(STEP_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .statusIcon(AMBIGUOUS, "✘")
                .style(STATUS_ICON, AMBIGUOUS, Ansi.with(FOREGROUND_RED), Ansi.with(FOREGROUND_DEFAULT))
                .statusIcon(FAILED, "✘")
                .style(STATUS_ICON, FAILED, Ansi.with(FOREGROUND_RED), Ansi.with(FOREGROUND_DEFAULT))
                .statusIcon(PASSED, "✔")
                .style(STATUS_ICON, PASSED, Ansi.with(FOREGROUND_GREEN), Ansi.with(FOREGROUND_DEFAULT))
                .statusIcon(PENDING, "■")
                .style(STATUS_ICON, PENDING, Ansi.with(FOREGROUND_YELLOW), Ansi.with(FOREGROUND_DEFAULT))
                .statusIcon(SKIPPED, "↷")
                .style(STATUS_ICON, SKIPPED, Ansi.with(FOREGROUND_CYAN), Ansi.with(FOREGROUND_DEFAULT))
                .statusIcon(UNDEFINED, "■")
                .style(STATUS_ICON, UNDEFINED, Ansi.with(FOREGROUND_YELLOW), Ansi.with(FOREGROUND_DEFAULT))
                .progressIcon(AMBIGUOUS, "A")
                .style(PROGRESS_ICON, AMBIGUOUS, Ansi.with(FOREGROUND_RED), Ansi.with(FOREGROUND_DEFAULT))
                .progressIcon(FAILED, "F")
                .style(PROGRESS_ICON, FAILED, Ansi.with(FOREGROUND_RED), Ansi.with(FOREGROUND_DEFAULT))
                .progressIcon(PASSED, ".")
                .style(PROGRESS_ICON, PASSED, Ansi.with(FOREGROUND_GREEN), Ansi.with(FOREGROUND_DEFAULT))
                .progressIcon(PENDING, "P")
                .style(PROGRESS_ICON, PENDING, Ansi.with(FOREGROUND_YELLOW), Ansi.with(FOREGROUND_DEFAULT))
                .progressIcon(SKIPPED, "-")
                .style(PROGRESS_ICON, SKIPPED, Ansi.with(FOREGROUND_CYAN), Ansi.with(FOREGROUND_DEFAULT))
                .progressIcon(UNDEFINED, "U")
                .style(PROGRESS_ICON, UNDEFINED, Ansi.with(FOREGROUND_YELLOW), Ansi.with(FOREGROUND_DEFAULT))
                .bulletPointIcon("•")
                .build();
    }

    /**
     * Empty theme that does not apply any styling to the output.
     */
    public static Theme none() {
        return Theme.builder().build();
    }

    /**
     * A plain text theme. Does not use any ANSI.
     */
    public static Theme plain() {
        return Theme.builder()
                .statusIcon(AMBIGUOUS, "✘")
                .statusIcon(FAILED, "✘")
                .statusIcon(PASSED, "✔")
                .statusIcon(PENDING, "■")
                .statusIcon(SKIPPED, "↷")
                .statusIcon(UNDEFINED, "■")
                .progressIcon(AMBIGUOUS, "A")
                .progressIcon(FAILED, "F")
                .progressIcon(PASSED, ".")
                .progressIcon(PENDING, "P")
                .progressIcon(SKIPPED, "-")
                .progressIcon(UNDEFINED, "U")
                .bulletPointIcon("-")
                .build();
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

    String style(Element element, TestStepResultStatus status, String text) {
        Entry<Ansi, Ansi> ansiStyle = findAnsiBy(element, status);
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

    String progressIcon(TestStepResultStatus status) {
        // Progress icons are assumed to be 1 character wide by default.
        return progressIconByStatus.getOrDefault(status, " ");
    }

    String statusIcon(TestStepResultStatus status) {
        // Status icons are assumed to be 1 character wide by default.
        return statusIconByStatus.getOrDefault(status, " ");
    }

    String bulletPointIcon() {
        return bulletPointIcon == null ? " " : bulletPointIcon;
    }

    private Entry<Ansi, Ansi> findAnsiBy(Element element) {
        return styleByElement.get(element);
    }

    private Entry<Ansi, Ansi> findAnsiBy(Element element, TestStepResultStatus status) {
        Map<TestStepResultStatus, Entry<Ansi, Ansi>> styleByStatus = styleByStatusByElement.get(element);
        return styleByStatus == null ? null : styleByStatus.get(status);
    }

    boolean hasStatusIcons() {
        return !statusIconByStatus.isEmpty();
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
         * The progress icon.
         * <p>
         * Always used in combination with a {@link TestStepResultStatus}.
         */
        PROGRESS_ICON,

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
         * The status icon.
         * <p>
         * Always used in combination with a {@link TestStepResultStatus}.
         */
        STATUS_ICON,

        /**
         * The step line.
         * <p>
         * Always used in combination with a {@link TestStepResultStatus}.
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

    public final static class Builder {
        private final EnumMap<TestStepResultStatus, String> statusIconByStatus = new EnumMap<>(TestStepResultStatus.class);
        private final EnumMap<TestStepResultStatus, String> progressIconByStatus = new EnumMap<>(TestStepResultStatus.class);
        private final EnumMap<Element, Entry<Ansi, Ansi>> styleByElement = new EnumMap<>(Element.class);
        private final EnumMap<Element, Map<TestStepResultStatus, Entry<Ansi, Ansi>>> styleByStatusByElement = new EnumMap<>(Element.class);
        private String bulletPointIcon;

        private Builder() {

        }

        /**
         * Adds a style and reset style for an element.
         *
         * @param element    the element to style
         * @param style      the ansi style to apply
         * @param resetStyle the ansi style to reset the applied styling
         * @return this builder
         */
        public Builder style(Element element, Ansi style, Ansi resetStyle) {
            requireNonNull(element);
            requireNonNull(style);
            requireNonNull(resetStyle);

            styleByElement.put(element, new AbstractMap.SimpleEntry<>(style, resetStyle));
            return this;
        }

        /**
         * Adds a status icon for the given status.
         * <p>
         * Visually the status icon must be 1-space wide.
         *
         * @param status the status for which the icon is used
         * @param icon   the icon
         * @return this builder
         */
        public Builder statusIcon(TestStepResultStatus status, String icon) {
            requireNonNull(status);
            requireNonNull(icon);
            statusIconByStatus.put(status, icon);
            return this;
        }

        /**
         * Adds a progress icon for the given status.
         * <p>
         * Visually the status icon must be 1-space wide.
         *
         * @param status the status for which the icon is used
         * @param icon   the icon
         * @return this builder
         */
        public Builder progressIcon(TestStepResultStatus status, String icon) {
            requireNonNull(status);
            requireNonNull(icon);
            progressIconByStatus.put(status, icon);
            return this;
        }

        /**
         * Adds a bullet point icon.
         * <p>
         * Visually the bullet point icon must be 1-space wide.
         *
         * @param icon   the icon
         * @return this builder
         */
        public Builder bulletPointIcon(String icon) {
            requireNonNull(icon);
            this.bulletPointIcon = icon;
            return this;
        }

        /**
         * Adds a style and reset style for an element.
         *
         * @param element    the element to style
         * @param status     the status of the element to style
         * @param style      the ansi style to apply
         * @param resetStyle the ansi style to reset the applied styling
         * @return this builder
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
            return new Theme(
                    new EnumMap<>(styleByElement),
                    new EnumMap<>(styleByStatusByElement),
                    new EnumMap<>(statusIconByStatus),
                    new EnumMap<>(progressIconByStatus),
                    bulletPointIcon
            );
        }

    }

}
