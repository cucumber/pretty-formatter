package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;

import java.util.function.Consumer;

import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT_BASE64;
import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT_PLAIN_TEXT;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION_COMMENT;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_TITLE;
import static io.cucumber.prettyformatter.Theme.Element.STEP;
import static io.cucumber.prettyformatter.Theme.Element.STEP_ARGUMENT;
import static io.cucumber.prettyformatter.Theme.Element.STEP_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.STEP_TEXT;
import static java.lang.System.lineSeparator;

class LineBuilder {

    private final StringBuilder builder = new StringBuilder(80);
    private final Theme theme;
    private int unstyledLength;

    LineBuilder(Theme theme, PrettyReportData data) {
        this.theme = theme;
    }

    LineBuilder indent(String indent) {
        append(indent);
        return this;
    }

    LineBuilder scenario(String keyword, String title) {
        append(SCENARIO_KEYWORD, keyword + ":");
        append(SCENARIO_TITLE, " " + title);
        return this;
    }

    LineBuilder location(int commentStartAtIndex, String location) {
        if (location.isEmpty()) {
            return this;
        }
        append(createPadding(commentStartAtIndex));
        append(LOCATION_COMMENT, "# " + location);
        return this;
    }

    private String createPadding(int commentStartAtIndex) {
        int padding = commentStartAtIndex - unstyledLength;

        if (padding <= 0) {
            return " ";
        }
        StringBuilder builder = new StringBuilder(padding);
        for (int i = 0; i < padding; i++) {
            builder.append(" ");
        }
        return builder.toString();
    }

    LineBuilder step(TestStepResultStatus status, String keyword, Consumer<LineBuilder> stepBuilder) {
        builder.append(theme.startStyle(STEP, status));
        append(STEP_KEYWORD, keyword);
        stepBuilder.accept(this);
        builder.append(theme.endStyle(STEP, status));
        return this;
    }

    LineBuilder stepText(String text) {
        append(STEP_TEXT, text);
        return this;
    }

    LineBuilder stepArgument(String text) {
        append(STEP_ARGUMENT, text);
        return this;
    }

    LineBuilder attachmentBase64(String text) {
        append(ATTACHMENT_BASE64, text);
        return this;
    }
    LineBuilder attachmentPlainText(String text) {
        append(ATTACHMENT_PLAIN_TEXT, text);
        return this;
    }

    LineBuilder newLine() {
        this.unstyledLength = 0;
        builder.append(lineSeparator());
        return this;
    }

    LineBuilder error(TestStepResultStatus status, String indented) {
        builder.append(theme.startStyle(STEP, status));
        append(indented);
        builder.append(theme.endStyle(STEP, status));
        return this;
    }
    
    private void append(String text) {
        this.unstyledLength += text.length();
        builder.append(text);
    }

    private void append(Theme.Element style, String text) {
        this.unstyledLength += text.length();
        builder.append(theme.style(style, text));
    }

    public String build() {
        return builder.toString();
    }

}
