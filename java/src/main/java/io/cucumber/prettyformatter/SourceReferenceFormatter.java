package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.SourceReference;

import java.util.Optional;
import java.util.function.Function;

final class SourceReferenceFormatter {
    private final Function<String, String> uriFormatter;

    SourceReferenceFormatter(Function<String, String> uriFormatter) {
        this.uriFormatter = uriFormatter;
    }

    Optional<String> format(SourceReference sourceReference) {
        if (sourceReference.getJavaMethod().isPresent()) {
            return sourceReference.getJavaMethod()
                    .map(javaMethod -> "%s.%s(%s)".formatted(
                            javaMethod.getClassName(),
                            javaMethod.getMethodName(),
                            String.join(",", javaMethod.getMethodParameterTypes())
                    ));
        }
        if (sourceReference.getJavaStackTraceElement().isPresent()) {
            return sourceReference.getJavaStackTraceElement()
                    .map(javaStackTraceElement -> "%s.%s(%s%s)".formatted(
                            javaStackTraceElement.getClassName(),
                            javaStackTraceElement.getMethodName(),
                            javaStackTraceElement.getFileName(),
                            sourceReference.getLocation().map(Location::getLine).map(line -> ":" + line).orElse("")
                    ));
        }
        if (sourceReference.getUri().isPresent()) {
            return sourceReference.getUri()
                    .map(uri -> uriFormatter.apply(uri) + sourceReference.getLocation()
                            .map(location -> ":" + location.getLine())
                            .orElse(""));
        }
        return Optional.empty();
    }
}
