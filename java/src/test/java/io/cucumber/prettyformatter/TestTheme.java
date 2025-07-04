package io.cucumber.prettyformatter;

import static io.cucumber.messages.types.TestStepResultStatus.AMBIGUOUS;
import static io.cucumber.messages.types.TestStepResultStatus.FAILED;
import static io.cucumber.messages.types.TestStepResultStatus.PASSED;
import static io.cucumber.messages.types.TestStepResultStatus.PENDING;
import static io.cucumber.messages.types.TestStepResultStatus.SKIPPED;
import static io.cucumber.messages.types.TestStepResultStatus.UNDEFINED;
import static io.cucumber.prettyformatter.Ansi.Attributes.BOLD;
import static io.cucumber.prettyformatter.Ansi.Attributes.BOLD_OFF;
import static io.cucumber.prettyformatter.Ansi.Attributes.FAINT;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_BRIGHT_BLACK;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_CYAN;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_GREEN;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_RED;
import static io.cucumber.prettyformatter.Ansi.Attributes.FOREGROUND_YELLOW;
import static io.cucumber.prettyformatter.Ansi.Attributes.ITALIC;
import static io.cucumber.prettyformatter.Ansi.Attributes.ITALIC_OFF;
import static io.cucumber.prettyformatter.Ansi.Attributes.RESET;
import static io.cucumber.prettyformatter.Ansi.Attributes.UNDERLINE;
import static io.cucumber.prettyformatter.Ansi.Attributes.UNDERLINE_OFF;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE_BORDER;
import static io.cucumber.prettyformatter.Theme.Element.DATA_TABLE_CONTENT;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING;
import static io.cucumber.prettyformatter.Theme.Element.DOC_STRING_CONTENT;
import static io.cucumber.prettyformatter.Theme.Element.LOCATION;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_KEYWORD;
import static io.cucumber.prettyformatter.Theme.Element.SCENARIO_NAME;
import static io.cucumber.prettyformatter.Theme.Element.STEP;
import static io.cucumber.prettyformatter.Theme.Element.TAG;

public class TestTheme {
    static Theme cucumberJs(){
        return Theme.builder()
                .style(DATA_TABLE, Ansi.with(FOREGROUND_BRIGHT_BLACK), Ansi.with(RESET))
                .style(DATA_TABLE_BORDER, Ansi.with(ITALIC), Ansi.with(ITALIC_OFF))
                .style(DATA_TABLE_CONTENT, Ansi.with(ITALIC), Ansi.with(ITALIC_OFF))
                .style(DOC_STRING, Ansi.with(FOREGROUND_BRIGHT_BLACK), Ansi.with(RESET))
                .style(DOC_STRING_CONTENT, Ansi.with(ITALIC), Ansi.with(ITALIC_OFF))
                .style(LOCATION, Ansi.with(FAINT), Ansi.with(RESET))
                .style(SCENARIO, Ansi.with(FOREGROUND_CYAN), Ansi.with(RESET))
                .style(SCENARIO_KEYWORD, Ansi.with(BOLD), Ansi.with(BOLD_OFF))
                .style(SCENARIO_NAME, Ansi.with(UNDERLINE), Ansi.with(UNDERLINE_OFF))

                .style(STEP, AMBIGUOUS, Ansi.with(FOREGROUND_RED), Ansi.with(RESET))
                .style(STEP, FAILED, Ansi.with(FOREGROUND_RED), Ansi.with(RESET))
                .style(STEP, PASSED, Ansi.with(FOREGROUND_GREEN), Ansi.with(RESET))
                .style(STEP, PENDING, Ansi.with(FOREGROUND_YELLOW), Ansi.with(RESET))
                .style(STEP, SKIPPED, Ansi.with(FOREGROUND_CYAN), Ansi.with(RESET))
                .style(STEP, UNDEFINED, Ansi.with(FOREGROUND_YELLOW), Ansi.with(RESET))
                
//                .with(STEP_KEYWORD, Ansi.with(FOREGROUND_CYAN), Ansi.with(RESET))
                .style(TAG, Ansi.with(FOREGROUND_CYAN), Ansi.with(RESET))
                .build();
    }
}
