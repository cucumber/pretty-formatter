package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;

import java.util.EnumMap;
import java.util.Map;

import static io.cucumber.messages.types.TestStepResultStatus.AMBIGUOUS;
import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static io.cucumber.messages.types.TestStepResultStatus.PASSED;
import static io.cucumber.messages.types.TestStepResultStatus.PENDING;
import static io.cucumber.messages.types.TestStepResultStatus.SKIPPED;
import static io.cucumber.messages.types.TestStepResultStatus.UNDEFINED;
import static io.cucumber.prettyformatter.AnsiStyle.FOREGROUND_BLUE;
import static io.cucumber.prettyformatter.AnsiStyle.FOREGROUND_BRIGHT_BLACK;
import static io.cucumber.prettyformatter.AnsiStyle.FOREGROUND_CYAN;
import static io.cucumber.prettyformatter.AnsiStyle.FOREGROUND_GREEN;
import static io.cucumber.prettyformatter.AnsiStyle.FOREGROUND_RED;
import static io.cucumber.prettyformatter.AnsiStyle.FOREGROUND_YELLOW;
import static io.cucumber.prettyformatter.AnsiStyle.INTENSITY_BOLD;
import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT_BASE64;
import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT_PLAIN_TEXT;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION_COMMENT;
import static io.cucumber.prettyformatter.Theme.Element.STEP;
import static io.cucumber.prettyformatter.Theme.Element.STEP_ARGUMENT;

public final class Theme {

    private final Map<Element, AnsiStyle> styleByElement;
    private final Map<Element, Map<TestStepResultStatus, AnsiStyle>> styleByStatusByElement;

    private Theme(Map<Element, AnsiStyle> styleByElement, Map<Element, Map<TestStepResultStatus, AnsiStyle>> styleByStatusByElement) {
        this.styleByElement = styleByElement;
        this.styleByStatusByElement = styleByStatusByElement;
    }

    public static Theme color() {
        return Theme.builder()
                .with(LOCATION_COMMENT, FOREGROUND_BRIGHT_BLACK)
                .with(STEP, UNDEFINED, FOREGROUND_YELLOW)
                .with(STEP, PENDING, FOREGROUND_YELLOW)
                .with(STEP, FAILED, FOREGROUND_RED)
                .with(STEP, AMBIGUOUS, FOREGROUND_RED)
                .with(STEP, PASSED, FOREGROUND_GREEN)
                .with(STEP, SKIPPED, FOREGROUND_CYAN)
                .with(STEP_ARGUMENT, INTENSITY_BOLD)
                .with(ATTACHMENT_BASE64, FOREGROUND_BLUE)
                .with(ATTACHMENT_PLAIN_TEXT, FOREGROUND_BLUE)
                .build();
    }

    public static Theme monochrome() {
        return Theme.builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    String style(Element element, String text) {
        AnsiStyle ansiStyle = styleByElement.get(element);
        if (ansiStyle == null) {
            return text;
        }
        return ansiStyle.getStartControlSequence() + text + ansiStyle.getEndControlSequence();
    }

    String startStyle(Element element, TestStepResultStatus status) {
        Map<TestStepResultStatus, AnsiStyle> a = styleByStatusByElement.get(element);
        if (a == null) {
            return "";
        }
        AnsiStyle b = a.get(status);
        if (b == null) {
            return "";
        }
        return b.getStartControlSequence();
    }

    String endStyle(Element element, TestStepResultStatus status) {
        Map<TestStepResultStatus, AnsiStyle> a = styleByStatusByElement.get(element);
        if (a == null) {
            return "";
        }
        AnsiStyle b = a.get(status);
        if (b == null) {
            return "";
        }
        return b.getEndControlSequence();
    }

    enum Element {
        SCENARIO,
        SCENARIO_KEYWORD,
        SCENARIO_TITLE,
        STEP,
        STEP_KEYWORD,
        STEP_TEXT,
        STEP_ARGUMENT,
        LOCATION_COMMENT,
        ATTACHMENT_BASE64,
        ATTACHMENT_PLAIN_TEXT,
    }

    public static class Builder {
        private final Map<Element, AnsiStyle> styleByElement = new EnumMap<>(Element.class);
        private final Map<Element, Map<TestStepResultStatus, AnsiStyle>> styleByStatusByElement = new EnumMap<>(Element.class);

        Builder with(Element element, AnsiStyle style) {
            styleByElement.put(element, style);
            return this;
        }

        Builder with(Element element, TestStepResultStatus status, AnsiStyle style) {
            Map<TestStepResultStatus, AnsiStyle> styleByStatus = styleByStatusByElement.computeIfAbsent(element, e -> new EnumMap<>(TestStepResultStatus.class));
            styleByStatus.put(status, style);
            return this;
        }

        Theme build() {
            return new Theme(styleByElement, styleByStatusByElement);
        }

    }

}
