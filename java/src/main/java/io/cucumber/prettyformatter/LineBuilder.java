package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;
import io.cucumber.prettyformatter.Theme.Element;

import java.util.function.Consumer;

import static java.lang.System.lineSeparator;

final class LineBuilder {

    private final StringBuilder builder = new StringBuilder(80);
    private final Theme theme;
    private int unstyledLength;

    LineBuilder(Theme theme) {
        this.theme = theme;
    }

    LineBuilder indent(int indent) {
        return append(createPadding(indent));
    }

    LineBuilder title(Element keywordElement, String keyword, Element nameElement, String name) {
        return append(keywordElement, keyword + ":")
                .append(" ")
                .append(nameElement, name);
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

    LineBuilder accept(Consumer<LineBuilder> consumer) {
        consumer.accept(this);
        return this;
    }

    LineBuilder newLine() {
        unstyledLength = 0;
        builder.append(lineSeparator());
        return this;
    }

    LineBuilder append(String text) {
        this.unstyledLength += text.length();
        builder.append(text);
        return this;
    }

    LineBuilder append(Element element, String text) {
        this.unstyledLength += text.length();
        builder.append(theme.style(element, text));
        return this;
    }

    LineBuilder begin(Element element) {
        builder.append(theme.beginStyle(element));
        return this;
    }

    LineBuilder begin(Element element, TestStepResultStatus status) {
        builder.append(theme.beginStyle(element, status));
        return this;
    }

    LineBuilder end(Element element) {
        builder.append(theme.endStyle(element));
        return this;
    }

    LineBuilder end(Element element, TestStepResultStatus status) {
        builder.append(theme.endStyle(element, status));
        return this;
    }

    String build() {
        return builder.toString();
    }

    @Override
    public String toString() {
        return builder.toString();
    }

}
