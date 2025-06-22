package io.cucumber.prettyformatter;

import io.cucumber.messages.types.TestStepResultStatus;

class NoAnsiFormatter implements Formatter {

    @Override
    public String comment(String text) {
        return text;
    }

    @Override
    public String step(TestStepResultStatus status, String text) {
        return text;
    }

    @Override
    public String error(TestStepResultStatus status, String text) {
        return text;
    }

    @Override
    public String argument(String text) {
        return text;
    }

    @Override
    public String output(String text) {
        return text;
    }
}
