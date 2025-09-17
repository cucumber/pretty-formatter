package io.cucumber.prettyformatter;

import java.util.Comparator;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

final class OrderableEvent<T> implements Comparable<OrderableEvent<T>> {
    private final T event;
    private final String uri;
    private final Long line;

    OrderableEvent(T event, String uri, Long line) {
        this.event = event;
        this.uri = uri;
        this.line = line;
    }

    private final Comparator<OrderableEvent<T>> comparator = Comparator
            .comparing((OrderableEvent<T> ord) -> ord.uri, nullsFirst(naturalOrder()))
            .thenComparing(ord -> ord.line, nullsFirst(naturalOrder()));

    @Override
    public int compareTo(OrderableEvent<T> o) {
        return comparator.compare(this, o);
    }

    T getEvent() {
        return event;
    }
}
