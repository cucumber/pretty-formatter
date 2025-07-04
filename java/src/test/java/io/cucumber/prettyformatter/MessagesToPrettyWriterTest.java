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

class MessagesToPrettyWriterTest {

    @Test
    void it_writes_two_messages_to_pretty() throws IOException {
        Instant started = Instant.ofEpochSecond(10);
        Instant finished = Instant.ofEpochSecond(30);

        String html = renderAsPretty(
                Envelope.of(new TestRunStarted(toMessage(started), "some-id")),
                Envelope.of(new TestRunFinished(null, true, toMessage(finished), null, "some-id")));

        assertThat(html).isEmpty();
    }

    @Test
    void it_writes_no_message_to_pretty() throws IOException {
        String html = renderAsPretty();
        assertThat(html).isEmpty();
    }

    @Test
    void it_throws_when_writing_after_close() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessagesToPrettyWriter messagesToHtmlWriter = create(bytes);
        messagesToHtmlWriter.close();
        assertThrows(IOException.class, () -> messagesToHtmlWriter.write(null));
    }

    @Test
    void it_can_be_closed_twice() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        MessagesToPrettyWriter messagesToHtmlWriter = create(bytes);
        messagesToHtmlWriter.close();
        assertDoesNotThrow(messagesToHtmlWriter::close);
    }

    private static String renderAsPretty(Envelope... messages) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MessagesToPrettyWriter messagesToHtmlWriter = create(bytes)) {
            for (Envelope message : messages) {
                messagesToHtmlWriter.write(message);
            }
        }

        return new String(bytes.toByteArray(), UTF_8);
    }

    private static MessagesToPrettyWriter create(ByteArrayOutputStream bytes) {
        return MessagesToPrettyWriter.builder().build(bytes);
    }
}
