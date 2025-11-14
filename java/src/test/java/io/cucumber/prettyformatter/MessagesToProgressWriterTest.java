package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Duration;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestRunStarted;
import io.cucumber.messages.types.TestStepFinished;
import io.cucumber.messages.types.TestStepResult;
import io.cucumber.messages.types.TestStepResultStatus;
import io.cucumber.messages.types.Timestamp;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

import static io.cucumber.messages.Convertor.toMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessagesToProgressWriterTest {

    @Test
    void it_writes_two_messages_to_progress() throws IOException {
        Instant started = Instant.ofEpochSecond(10);
        Instant finished = Instant.ofEpochSecond(30);

        String progress = renderAsProgress(
                Envelope.of(new TestRunStarted(toMessage(started), "some-id")),
                Envelope.of(new TestRunFinished(null, true, toMessage(finished), null, "some-id")));

        assertThat(progress).isEqualToNormalizingNewlines("\n");
    }

    @Test
    void it_writes_no_message_to_progress() throws IOException {
        String progress = renderAsProgress();
        assertThat(progress).isEmpty();
    }

    @Test
    void it_throws_when_writing_after_close() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessagesToProgressWriter writer = MessagesToProgressWriter.builder().build(bytes);
        writer.close();
        assertThrows(IOException.class, () -> writer.write(
                Envelope.of(new TestRunStarted(new Timestamp(0L, 0), ""))
        ));
    }

    @Test
    void it_can_be_closed_twice() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessagesToProgressWriter writer = MessagesToProgressWriter.builder().build(bytes);
        writer.close();
        assertDoesNotThrow(writer::close);
    }

    @Test
    void it_renders_max_80_characters_per_line_by_default() throws IOException {
        Envelope envelope = Envelope.of(new TestStepFinished(
                "",
                "",
                new TestStepResult(
                        new Duration(0L, 0),
                        null,
                        TestStepResultStatus.PASSED,
                        null

                ),
                new Timestamp(0L, 0)
        ));
        Envelope[] messages = new Envelope[128];
        Arrays.fill(messages, envelope);

        String progress = renderAsProgress(messages);
        assertThat(progress).containsPattern("^\\.{80}\n\\.{48}$");
    }

    @Test
    void it_renders_max_75_characters_per_line() throws IOException {
        Envelope envelope = Envelope.of(new TestStepFinished(
                "",
                "",
                new TestStepResult(
                        new Duration(0L, 0),
                        null,
                        TestStepResultStatus.PASSED,
                        null

                ),
                new Timestamp(0L, 0)
        ));
        Envelope[] messages = new Envelope[128];
        Arrays.fill(messages, envelope);

        String progress = renderAsProgress(builder().maxWidth(75), messages);
        assertThat(progress).containsPattern("^\\.{75}\n\\.{53}$");
    }

    private static String renderAsProgress(Envelope... messages) throws IOException {
        return renderAsProgress(builder(), messages);
    }

    private static String renderAsProgress(MessagesToProgressWriter.Builder builder, Envelope... messages) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MessagesToProgressWriter writer = builder.build(bytes)) {
            for (Envelope message : messages) {
                writer.write(message);
            }
        }

        return bytes.toString(UTF_8);
    }

    private static MessagesToProgressWriter.Builder builder() {
        return MessagesToProgressWriter.builder();
    }

}
