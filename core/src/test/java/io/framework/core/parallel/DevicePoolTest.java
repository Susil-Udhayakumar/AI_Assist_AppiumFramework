package io.framework.core.parallel;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

class DevicePoolTest {

    @Test
    void acquireReturnsAvailableDevice() throws Exception {
        var pool = new DevicePool(List.of(
                new DeviceLease("emulator-5554", 8200, "local")));
        DeviceLease lease = pool.acquire(1, TimeUnit.SECONDS);
        assertThat(lease.deviceId()).isEqualTo("emulator-5554");
    }

    @Test
    void acquireBlocksUntilReleaseWhenExhausted() throws Exception {
        var pool = new DevicePool(List.of(new DeviceLease("d1", 8200, "local")));
        DeviceLease first = pool.acquire(1, TimeUnit.SECONDS);

        var executor = Executors.newSingleThreadExecutor();
        Future<DeviceLease> waiting = executor.submit(() -> pool.acquire(2, TimeUnit.SECONDS));

        Thread.sleep(200);
        assertThat(waiting.isDone()).isFalse();   // still blocked, pool empty

        pool.release(first);
        assertThat(waiting.get(1, TimeUnit.SECONDS).deviceId()).isEqualTo("d1");
        executor.shutdownNow();
    }

    @Test
    void neverHandsSameDeviceToTwoThreads() throws Exception {
        int devices = 4, workers = 16;
        var leases = new java.util.ArrayList<DeviceLease>();
        for (int i = 0; i < devices; i++) leases.add(new DeviceLease("d" + i, 8200 + i, "local"));
        var pool = new DevicePool(leases);

        var concurrentHolders = new AtomicInteger();
        var maxConcurrent = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(workers);
        var tasks = new java.util.ArrayList<Callable<Void>>();
        for (int i = 0; i < workers; i++) {
            tasks.add(() -> {
                DeviceLease l = pool.acquire(5, TimeUnit.SECONDS);
                int now = concurrentHolders.incrementAndGet();
                maxConcurrent.accumulateAndGet(now, Math::max);
                Thread.sleep(20);
                concurrentHolders.decrementAndGet();
                pool.release(l);
                return null;
            });
        }
        executor.invokeAll(tasks);
        executor.shutdown();

        assertThat(maxConcurrent.get()).isLessThanOrEqualTo(devices);
    }
}
