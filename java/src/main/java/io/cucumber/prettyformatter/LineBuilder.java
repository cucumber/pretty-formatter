package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;
import io.cucumber.prettyformatter.Theme.Element;

import java.util.function.Consumer;

import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE_BORDER;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE_CONTENT;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_CONTENT;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_CONTENT_TYPE;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_DELIMITER;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_NAME;
import static io.cucumber.prettyformatter.Theme.Element.STEP;
import static io.cucumber.prettyformatter.Theme.Element.STEP_ARGUMENT;
import static io.cucumber.prettyformatter.Theme.Element.STEP_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.STEP_TEXT;
import static io.cucumber.prettyformatter.Theme.Element.TAG;
import static java.lang.System.lineSeparator;

class LineBuilder {

    private final StringBuilder builder = new StringBuilder(80);
    private final Theme theme;
    private int unstyledLength;

    LineBuilder(Theme theme) {
        this.theme = theme;
    }

    LineBuilder indent(String indent) {
        return append(indent);
    }

    LineBuilder tag(String text) {
        return append(TAG, text);
    }

    LineBuilder scenario(String keyword, String title) {
        return append(SCENARIO_KEYWORD, keyword + ":")
                .append(" ")
                .append(SCENARIO_NAME, title);
    }

    LineBuilder location(String location) {
        return append(LOCATION, "# " + location);
    }

    LineBuilder addPaddingUpTo(int index) {
        return append(createPadding(index));
    }

    private String createPadding(int commentStartAtIndex) {
        int padding = commentStartAtIndex - unstyledLength;
        StringBuilder builder = new StringBuilder(padding);
        for (int i = 0; i < padding; i++) {
            builder.append(" ");
        }
        return builder.toString();
    }

    LineBuilder stepKeyword(String keyword) {
        return append(STEP_KEYWORD, keyword);
    }

    LineBuilder beginStep(TestStepResultStatus status) {
        return begin(STEP, status);
    }

    LineBuilder endStep(TestStepResultStatus status) {
        return end(STEP, status);
    }

    LineBuilder accept(Consumer<LineBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    LineBuilder stepText(String text) {
        return append(STEP_TEXT, text);
    }

    LineBuilder stepArgument(String text) {
        return append(STEP_ARGUMENT, text);
    }

    LineBuilder attachment(String text) {
        return append(ATTACHMENT, text);
    }

    LineBuilder newLine() {
        unstyledLength = 0;
        builder.append(lineSeparator());
        return this;
    }

    LineBuilder error(TestStepResultStatus status, String text) {
        return beginStep(status)
                .append(text)
                .endStep(status);
    }

    LineBuilder beginDocString() {
        return begin(DOC_STRING);
    }

    LineBuilder endDocString() {
        return end(DOC_STRING);
    }

    LineBuilder docStringDelimiter(String text) {
        return append(DOC_STRING_DELIMITER, text);
    }

    LineBuilder docStringContentType(String text) {
        return append(DOC_STRING_CONTENT_TYPE, text);
    }

    LineBuilder docStringContent(String text) {
        return append(DOC_STRING_CONTENT, text);
    }

    LineBuilder beginDataTable() {
        return begin(DATA_TABLE);
    }

    LineBuilder endDataTable() {
        return end(DATA_TABLE);
    }

    LineBuilder tableDataBorder(String text) {
        return append(DATA_TABLE_BORDER, text);
    }

    LineBuilder dataTableContent(String text) {
        return append(DATA_TABLE_CONTENT, text);
    }

    private LineBuilder append(String text) {
        this.unstyledLength += text.length();
        builder.append(text);
        return this;
    }

    private LineBuilder append(Element element, String text) {
        this.unstyledLength += text.length();
        builder.append(theme.style(element, text));
        return this;
    }

    private LineBuilder begin(Element element) {
        builder.append(theme.beginStyle(element));
        return this;
    }

    private LineBuilder begin(Element element, TestStepResultStatus status) {
        builder.append(theme.beginStyle(element, status));
        return this;
    }

    private LineBuilder end(Element element) {
        builder.append(theme.endStyle(element));
        return this;
    }

    private LineBuilder end(Element element, TestStepResultStatus status) {
        builder.append(theme.endStyle(element, status));
        return this;
    }

    public String build() {
        return builder.toString();
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
