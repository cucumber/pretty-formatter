package io.cucumber.prettyformatter;

import static io.cucumber.messages.types.TestStepResultStatus.AMBIGUOUS;
import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static io.cucumber.messages.types.TestStepResultStatus.PASSED;
import static io.cucumber.messages.types.TestStepResultStatus.PENDING;
import static io.cucumber.messages.types.TestStepResultStatus.SKIPPED;
import static io.cucumber.messages.types.TestStepResultStatus.UNDEFINED;
import static io.cucumber.prettyformatter.Ansi.Attributes.BACKGROUND_BLUE;
import static io.cucumber.prettyformatter.Ansi.Attributes.BACKGROUND_DEFAULT;
import static io.cucumber.prettyformatter.Ansi.Attributes.BOLD;
import static io.cucumber.prettyformatter.Ansi.Attributes.BOLD_OFF;
import static io.cucumber.prettyformatter.Ansi.Attributes.FAINT;
import static io.cucumber.prettyformatter.Ansi.Attributes.FAINT_OFF;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_BLUE;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_BRIGHT_BLACK;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_CYAN;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_DEFAULT;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_GREEN;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_RED;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_YELLOW;
import static io.cucumber.prettyformatter.Ansi.Attributes.ITALIC;
import static io.cucumber.prettyformatter.Ansi.Attributes.ITALIC_OFF;
import static io.cucumber.prettyformatter.Theme.Element.ATTACHMENT;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE_BORDER;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE_CONTENT;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_CONTENT;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_DELIMITER;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_MEDIA_TYPE;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.FEATURE_NAME;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION;
import static io.cucumber.prettyformatter.Theme.Element.RULE;
import static io.cucumber.prettyformatter.Theme.Element.RULE_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.RULE_NAME;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_NAME;
import static io.cucumber.prettyformatter.Theme.Element.STEP;
import static io.cucumber.prettyformatter.Theme.Element.STEP_ARGUMENT;
import static io.cucumber.prettyformatter.Theme.Element.STEP_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.STEP_TEXT;
import static io.cucumber.prettyformatter.Theme.Element.TAG;

public class TestTheme {
    static Theme demo() {
        return Theme.builder()
                .style(ATTACHMENT, Ansi.with(FOREGROUND_BLUE), Ansi.with(FOREGROUND_DEFAULT))
                .style(DATA_TABLE, Ansi.with(FOREGROUND_BRIGHT_BLACK), Ansi.with(FOREGROUND_DEFAULT))
                .style(DATA_TABLE_BORDER, Ansi.with(FAINT), Ansi.with(FAINT_OFF))
                .style(DATA_TABLE_CONTENT, Ansi.with(ITALIC), Ansi.with(ITALIC_OFF))
                .style(DOC_STRING, Ansi.with(FOREGROUND_BRIGHT_BLACK), Ansi.with(FOREGROUND_DEFAULT))
                .style(DOC_STRING_CONTENT, Ansi.with(ITALIC), Ansi.with(ITALIC_OFF))
                .style(DOC_STRING_DELIMITER, Ansi.with(FAINT), Ansi.with(FAINT_OFF))
                .style(DOC_STRING_MEDIA_TYPE, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(FEATURE, Ansi.with(BACKGROUND_BLUE), Ansi.with(BACKGROUND_DEFAULT))
                .style(FEATURE_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(FEATURE_NAME, Ansi.with(ITALIC), Ansi.with(ITALIC_OFF))
                .style(LOCATION, Ansi.with(FOREGROUND_BRIGHT_BLACK), Ansi.with(FOREGROUND_DEFAULT))
                .style(RULE, Ansi.with(BACKGROUND_BLUE), Ansi.with(BACKGROUND_DEFAULT))
                .style(RULE_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(RULE_NAME, Ansi.with(ITALIC), Ansi.with(ITALIC_OFF))
                .style(SCENARIO, Ansi.with(BACKGROUND_BLUE), Ansi.with(BACKGROUND_DEFAULT))
                .style(SCENARIO_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(SCENARIO_NAME, Ansi.with(ITALIC), Ansi.with(ITALIC_OFF))
                .style(STEP, UNDEFINED, Ansi.with(FOREGROUND_YELLOW), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP, PENDING, Ansi.with(FOREGROUND_YELLOW), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP, FAILED, Ansi.with(FOREGROUND_RED), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP, AMBIGUOUS, Ansi.with(FOREGROUND_RED), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP, PASSED, Ansi.with(FOREGROUND_GREEN), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP, SKIPPED, Ansi.with(FOREGROUND_CYAN), Ansi.with(FOREGROUND_DEFAULT))
                .style(STEP_ARGUMENT, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(STEP_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(STEP_TEXT, Ansi.with(ITALIC), Ansi.with(ITALIC_OFF))
                .style(TAG, Ansi.with(FOREGROUND_YELLOW, BOLD), Ansi.with(BOLD_OFF, FOREGROUND_DEFAULT))
                .bulletPointIcon("•")
                .build();
    }
}
