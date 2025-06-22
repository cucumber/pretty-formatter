package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;

/**
 *  TODO: Don't expose. Use builder
 */
public interface Formatter {
    
    String comment(String text);
    
    String step(TestStepResultStatus status, String text);

    String error(TestStepResultStatus status, String text);
    
    String argument(String text);

    String output(String text);
    
    static Formatter ansi() {
        return new AnsiFormatter();
    }
    
    static Formatter noAnsi() {
        return new NoAnsiFormatter();
    }

}
