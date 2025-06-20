# Acceptance test data

The junit xml formatter uses the examples from the [cucumber compatibility kit](https://github.com/cucumber/compatibility-kit)
for acceptance testing. These examples consist of `.ndjson` files created by
the [`fake-cucumber` reference implementation](https://github.com/cucumber/fake-cucumber).

* The `.njdon` files are copied in by running `npm install`.
* The expected `.xml` files are created by running the
  `MessagesToJunitXmlWriterAcceptanceTest#updateExpectedXmlReportFiles` test.

We ensure the `.ndjson` files stay up to date by running `npm install` in CI
and verifying nothing changed.

Should there be changes, these tests can be used to update the expected data:
 * Java: `MessagesToJunitXmlWriterAcceptanceTest.updateExpectedXmlReportFiles`
