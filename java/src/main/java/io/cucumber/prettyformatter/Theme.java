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
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_BLUE;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_BRIGHT_BLACK;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_CYAN;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_GREEN;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_RED;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_YELLOW;
import static io.cucumber.prettyformatter.Ansi.Attributes.BOLD;
import static io.cucumber.prettyformatter.Ansi.Attributes.BOLD_OFF;
import static io.cucumber.prettyformatter.Ansi.Attributes.RESET;
import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION;
import static io.cucumber.prettyformatter.Theme.Element.STEP;
import static io.cucumber.prettyformatter.Theme.Element.STEP_ARGUMENT;
import static java.util.Objects.requireNonNull;

/**
 * An ANSI theme for the pretty report.
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

    public static Theme cucumberJvm() {
        return Theme.builder()
                .with(ATTACHMENT, Ansi.with(FOREGROUND_BLUE), Ansi.with(RESET))
                .with(LOCATION, Ansi.with(FOREGROUND_BRIGHT_BLACK), Ansi.with(RESET))
                .with(STEP, UNDEFINED, Ansi.with(FOREGROUND_YELLOW), Ansi.with(RESET))
                .with(STEP, PENDING, Ansi.with(FOREGROUND_YELLOW), Ansi.with(RESET))
                .with(STEP, FAILED, Ansi.with(FOREGROUND_RED), Ansi.with(RESET))
                .with(STEP, AMBIGUOUS, Ansi.with(FOREGROUND_RED), Ansi.with(RESET))
                .with(STEP, PASSED, Ansi.with(FOREGROUND_GREEN), Ansi.with(RESET))
                .with(STEP, SKIPPED, Ansi.with(FOREGROUND_CYAN), Ansi.with(RESET))
                .with(STEP_ARGUMENT, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .build();
    }

    public static Theme noColor() {
        return Theme.builder().build();
    }

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

    public enum Element {
        
        ATTACHMENT,
        DATA_TABLE,
        DATA_TABLE_BORDER,
        DATA_TABLE_CONTENT,
        DOC_STRING,
        DOC_STRING_CONTENT,
        DOC_STRING_CONTENT_TYPE,
        DOC_STRING_DELIMITER,
        FEATURE,
        FEATURE_KEYWORD,
        FEATURE_NAME,
        LOCATION,
        RULE,
        RULE_KEYWORD,
        RULE_NAME,
        SCENARIO,
        SCENARIO_KEYWORD,
        SCENARIO_NAME,
        STEP,
        STEP_KEYWORD,
        STEP_TEXT,
        STEP_ARGUMENT,
        TAG
    }

    public static class Builder {
        private final Map<Element, Entry<Ansi, Ansi>> styleByElement = new EnumMap<>(Element.class);
        private final Map<Element, Map<TestStepResultStatus, Entry<Ansi, Ansi>>> styleByStatusByElement = new EnumMap<>(Element.class);

        public Builder with(Element element, Ansi style, Ansi resetStyle) {
            requireNonNull(element);
            requireNonNull(style);
            requireNonNull(resetStyle);

            styleByElement.put(element, new AbstractMap.SimpleEntry<>(style, resetStyle));
            return this;
        }

        public Builder with(Element element, TestStepResultStatus status, Ansi style, Ansi resetStyle) {
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
