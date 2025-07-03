package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;

interface Formatter {

    String scenario(String text);
    
    String step(TestStepResultStatus status, String text);

    String argument(String text);

    String output(String text);

    String error(TestStepResultStatus status, String text);
    
    String comment(String text);
    
    static Formatter ansi() {
        return new AnsiFormatter();
    }
    
    static Formatter noAnsi() {
        return new NoAnsiFormatter();
    }

}
