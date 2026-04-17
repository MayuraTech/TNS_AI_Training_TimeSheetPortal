package com.tns.tms.shared.exception;

import java.time.Instant;

public class AccountLockedException extends TmsException {
    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("Account is locked until " + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
