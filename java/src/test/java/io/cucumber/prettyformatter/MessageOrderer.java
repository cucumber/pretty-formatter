package io.cucumber.prettyformatter;

import io.cucumber.messages.types.Envelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

class MessageOrderer {

    /**
     * Make test cases deterministically reproducible.
     */
    private static final Random random = new Random(202509171620L);

    static Consumer<List<Envelope>> originalOrder() {
        return envelopes -> {
        };
    }

    /**
     * Simulates parallel execution of testcases by interleaving the
     * execution of different test cases.
     */
    static Consumer<List<Envelope>> simulateParallelExecution() {
        return messages -> {
            List<Envelope> testCaseMessagesInSerial = testCasesSubList(messages);
            List<List<Envelope>> testCaseMessagesGrouped = groupTestCasesIntoBuckets(testCaseMessagesInSerial);
            List<Envelope> testCaseMessagesInterleaved = interleafMessagesFromTestCases(testCaseMessagesGrouped);
            replaceAll(testCaseMessagesInSerial, testCaseMessagesInterleaved);
        };
    }

    private static void replaceAll(List<Envelope> testCaseMessagesInSerial, List<Envelope> envelopes) {
        for (int i = 0; i < testCaseMessagesInSerial.size(); i++) {
            testCaseMessagesInSerial.set(i, envelopes.get(i));
        }
    }

    private static List<Envelope> interleafMessagesFromTestCases(List<List<Envelope>> messagesGroupedByTestCase) {
        List<Envelope> serial = new ArrayList<>();
        while (!messagesGroupedByTestCase.isEmpty()) {
            Collections.shuffle(messagesGroupedByTestCase, random);
            Iterator<List<Envelope>> bucketIterator = messagesGroupedByTestCase.iterator();
            while (bucketIterator.hasNext()) {
                List<Envelope> bucket = bucketIterator.next();
                if (bucket.isEmpty()) {
                    bucketIterator.remove();
                } else {
                    serial.add(bucket.remove(0));
                }
            }
        }
        return serial;
    }

    private static List<List<Envelope>> groupTestCasesIntoBuckets(List<Envelope> testCaseMessages) {
        List<List<Envelope>> buckets = new ArrayList<>();
        List<Envelope> currentBucket = new ArrayList<>();
        for (Envelope middleMessage : testCaseMessages) {
            if (middleMessage.getTestCaseStarted().isPresent()) {
                buckets.add(currentBucket);
                currentBucket = new ArrayList<>();
            }
            currentBucket.add(middleMessage);
        }
        buckets.add(currentBucket);
        return buckets;
    }

    private static List<Envelope> testCasesSubList(List<Envelope> messages) {
        int testRunStartedIndex = findTestRunStartedIndex(messages);
        int testRunFinishedIndex = findTestRunFinishedIndex(messages);
        return messages.subList(testRunStartedIndex + 1, testRunFinishedIndex - 1);
    }

    private static int findTestRunFinishedIndex(List<Envelope> messages) {
        int testRunFinishedIndex = messages.size() - 1;
        for (; testRunFinishedIndex >= 0; testRunFinishedIndex--) {
            if (messages.get(testRunFinishedIndex).getTestRunFinished().isPresent()) {
                break;
            }
        }
        return testRunFinishedIndex;
    }

    private static int findTestRunStartedIndex(List<Envelope> messages) {
        int testRunStartedIndex = 0;
        for (; testRunStartedIndex < messages.size(); testRunStartedIndex++) {
            if (messages.get(testRunStartedIndex).getTestRunStarted().isPresent()) {
                break;
            }
        }
        return testRunStartedIndex;
    }
}
