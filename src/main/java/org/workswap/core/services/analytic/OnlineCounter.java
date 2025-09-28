package org.workswap.core.services.analytic;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OnlineCounter {

    private final AtomicInteger online = new AtomicInteger(0);

    public int increment() {
        return online.incrementAndGet();
    }

    public int decrement() {
        return online.decrementAndGet();
    }

    public int getCurrent() {
        return online.get();
    }
}