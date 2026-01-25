package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.TestRunFinished;
import io.cucumber.messages.types.TestRunStarted;
import io.cucumber.messages.types.Timestamp;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import static io.cucumber.messages.Convertor.toMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessagesToPrettyWriterTest {

    @Test
    void it_writes_two_messages_to_pretty() throws IOException {
        Instant started = Instant.ofEpochSecond(10);
        Instant finished = Instant.ofEpochSecond(30);

        String out = renderAsPretty(
                Envelope.of(new TestRunStarted(toMessage(started), "some-id")),
                Envelope.of(new TestRunFinished(null, true, toMessage(finished), null, "some-id")));

        assertThat(out).isEmpty();
    }

    @Test
    void it_writes_no_message_to_pretty() throws IOException {
        String out = renderAsPretty();
        assertThat(out).isEmpty();
    }

    @Test
    void it_throws_when_writing_after_close() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessagesToPrettyWriter writer = create(bytes);
        writer.close();
        assertThrows(IOException.class, () -> writer.write(
                Envelope.of(new TestRunStarted(new Timestamp(0L, 0), ""))
        ));
    }

    @Test
    void it_can_be_closed_twice() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessagesToPrettyWriter writer = create(bytes);
        writer.close();
        assertDoesNotThrow(writer::close);
    }

    private static String renderAsPretty(Envelope... messages) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MessagesToPrettyWriter writer = create(bytes)) {
            for (Envelope message : messages) {
                writer.write(message);
            }
        }

        return bytes.toString(UTF_8);
    }

    private static MessagesToPrettyWriter create(ByteArrayOutputStream bytes) {
        return MessagesToPrettyWriter.builder().build(bytes);
    }
}
