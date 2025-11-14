package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestRunStarted;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import static io.cucumber.messages.Convertor.toMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessagesToSummaryWriterTest {

    @Test
    void it_writes_two_messages_to_summary() throws IOException {
        Instant started = Instant.ofEpochSecond(10);
        Instant finished = Instant.ofEpochSecond(30);

        String out = renderAsSummary(
                Envelope.of(new TestRunStarted(toMessage(started), "some-id")),
                Envelope.of(new TestRunFinished(null, true, toMessage(finished), null, "some-id")));

        assertThat(out).isEqualToNormalizingNewlines("""
                
                0 scenarios
                0 steps
                0m 20.0s
                """
        );
    }

    @Test
    void it_writes_no_message_to_summary() throws IOException {
        String out = renderAsSummary();
        assertThat(out).isEqualToNormalizingNewlines("""
                
                0 scenarios
                0 steps
                """);
    }

    @Test
    void it_throws_when_writing_after_close() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessagesToSummaryWriter writer = create(bytes);
        writer.close();
        assertThrows(IOException.class, () -> writer.write(
                Envelope.of(new TestRunStarted(toMessage(Instant.now()), "some-id"))
        ));
    }

    @Test
    void it_can_be_closed_twice() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessagesToSummaryWriter writer = create(bytes);
        writer.close();
        assertDoesNotThrow(writer::close);
    }

    private static String renderAsSummary(Envelope... messages) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MessagesToSummaryWriter writer = create(bytes)) {
            for (Envelope message : messages) {
                writer.write(message);
            }
        }

        return bytes.toString(UTF_8);
    }

    private static MessagesToSummaryWriter create(ByteArrayOutputStream bytes) {
        return MessagesToSummaryWriter.builder().build(bytes);
    }
}
