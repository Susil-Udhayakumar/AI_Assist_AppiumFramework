package io.framework.core.parallel;

import io.framework.core.exception.FrameworkException;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe pool of devices. acquire() blocks when none are free; release() returns one.
 * Guarantees no two workers hold the same lease (a lease is removed from the queue while held).
 */
public final class DevicePool {

    private final LinkedBlockingQueue<DeviceLease> available;
    private final int capacity;

    public DevicePool(Collection<DeviceLease> devices) {
        if (devices.isEmpty()) throw new FrameworkException("DevicePool requires at least one device");
        this.available = new LinkedBlockingQueue<>(devices);
        this.capacity = devices.size();
    }

    public DeviceLease acquire(long timeout, TimeUnit unit) {
        try {
            DeviceLease lease = available.poll(timeout, unit);
            if (lease == null) {
                throw new FrameworkException("No device available within "
                        + timeout + " " + unit.name().toLowerCase());
            }
            return lease;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FrameworkException("Interrupted while acquiring a device", e);
        }
    }

    public void release(DeviceLease lease) {
        available.offer(lease);
    }

    public int capacity() { return capacity; }
}
