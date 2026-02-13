# Acceptance test data

The pretty formatter uses the examples from the [Cucumber Compatibility Kit](https://github.com/cucumber/compatibility-kit)
for acceptance testing. These examples consist of `.ndjson` files created by
the CCK's reference implementation.

* The `.ndjson` files are copied in by running `npm install`.
* The expected `.log` files are created by running a test that updates them.

We ensure the `.ndjson` files stay up to date by running `npm install` in CI
and verifying nothing changed.

Should there be changes, these tests can be used to update the expected data:
 * Java: `MessagesToPrettyWriterAcceptanceTest#updateExpectedFiles`
 * Java: `MessagesToSummaryWriterAcceptanceTest#updateExpectedFiles`
 * Java: `MessagesToProgressWriterAcceptanceTest#updateExpectedFiles`
