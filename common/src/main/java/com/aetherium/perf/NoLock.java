package com.aetherium.perf;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Lock-free entity data (1.17.2): a read-write lock that does nothing, for objects that vanilla
 * guards with a real lock although only one thread ever uses them (an entity's synced data is
 * read and written by the thread that ticks the entity).
 */
public final class NoLock implements ReadWriteLock, Lock {
    public static final NoLock INSTANCE = new NoLock();

    private NoLock() {
    }

    @Override
    public Lock readLock() {
        return this;
    }

    @Override
    public Lock writeLock() {
        return this;
    }

    @Override
    public void lock() {
    }

    @Override
    public void lockInterruptibly() {
    }

    @Override
    public boolean tryLock() {
        return true;
    }

    @Override
    public boolean tryLock(final long time, final TimeUnit unit) {
        return true;
    }

    @Override
    public void unlock() {
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}
