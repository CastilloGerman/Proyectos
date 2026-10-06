package com.appgestion.api.service;

import java.util.concurrent.atomic.AtomicBoolean;

/** Reserva temporal de cuota. Solo commit() consume una unidad; close() libera las reservas fallidas. */
public final class AiRequestQuotaPermit implements AutoCloseable {

    private final Runnable commitAction;
    private final Runnable releaseAction;
    private final AtomicBoolean completed = new AtomicBoolean();

    AiRequestQuotaPermit(Runnable commitAction, Runnable releaseAction) {
        this.commitAction = commitAction;
        this.releaseAction = releaseAction;
    }

    public void commit() {
        if (completed.compareAndSet(false, true)) commitAction.run();
    }

    @Override
    public void close() {
        if (completed.compareAndSet(false, true)) releaseAction.run();
    }
}
