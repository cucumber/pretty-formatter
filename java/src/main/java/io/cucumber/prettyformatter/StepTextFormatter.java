package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Group;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.StepMatchArgument;
import io.cucumber.messages.types.TestStep;

import java.util.ArrayList;
import java.util.List;

import static io.cucumber.prettyformatter.Theme.Element.STEP_ARGUMENT;
import static io.cucumber.prettyformatter.Theme.Element.STEP_TEXT;
import static java.util.Collections.emptyList;

final class StepTextFormatter {

    StepTextFormatter() {
    }

    void formatTo(TestStep testStep, PickleStep pickleStep, LineBuilder lineBuilder) {
        formatStepText(lineBuilder, pickleStep.getText(), getStepMatchArguments(testStep));
    }

    private List<StepMatchArgument> getStepMatchArguments(TestStep testStep) {
        List<StepMatchArgument> stepMatchArguments = new ArrayList<>();
        testStep.getStepMatchArgumentsLists()
                .filter(stepMatchArgumentsList -> stepMatchArgumentsList.size() == 1)
                .orElse(emptyList())
                .forEach(list -> stepMatchArguments.addAll(list.getStepMatchArguments()));
        return stepMatchArguments;
    }

    private void formatStepText(LineBuilder lineBuilder, String stepText, List<StepMatchArgument> arguments) {
        int currentIndex = 0;
        for (StepMatchArgument argument : arguments) {
            Group group = argument.getGroup();
            // Ignore absent values, or groups without a start
            if (group.getValue().isPresent() && group.getStart().isPresent()) {
                String groupValue = group.getValue().get();
                // TODO: Messages are silly
                int groupStart = (int) (long) group.getStart().get();
                String text = stepText.substring(currentIndex, groupStart);
                currentIndex = groupStart + groupValue.length();
                lineBuilder.append(STEP_TEXT, text)
                        .append(STEP_ARGUMENT, groupValue);
            }
        }
        if (currentIndex != stepText.length()) {
            String remainder = stepText.substring(currentIndex);
            lineBuilder.append(STEP_TEXT, remainder);
        }
    }
}
