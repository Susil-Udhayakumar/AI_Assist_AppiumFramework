package io.framework.examples;

import io.framework.actions.ElementActions;
import io.framework.ai.heuristic.HeuristicElementHealer;
import io.framework.core.config.Capture;
import io.framework.core.config.Platform;
import io.framework.core.context.ContextManager;
import io.framework.core.context.DriverContext;
import io.framework.core.events.EventBus;
import io.framework.core.events.TestEvent;
import io.framework.core.parallel.DeviceLease;
import io.framework.core.parallel.DevicePool;
import io.framework.devices.DeviceProvider;
import io.framework.devices.DevicePools;
import io.framework.locators.LocatorRepository;
import io.framework.locators.LocatorStats;
import io.framework.locators.SmartFinder;
import io.framework.observability.CaptureLayout;
import io.framework.observability.CaptureListener;
import io.framework.observability.ScreenshotProvider;
import io.framework.observability.Screenshotter;
import io.framework.observability.SplitLogWriter;
import io.framework.reporting.Reporters;
import io.framework.reporting.SimpleHtmlReporter;
import io.framework.reporting.TestResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Exercises the whole v1 stack with no real device: devices -> DevicePool, core context +
 * EventBus, observability capture, locators smart-find + ai-heuristic healer, actions, and
 * reporting. Proves the modules integrate and produce artifacts end to end.
 */
class EndToEndSmokeTest {

    @Test
    void fullFlowProducesCaptureAndReport(@TempDir Path work) throws Exception {
        // devices -> pool
        DeviceProvider provider = new DeviceProvider() {
            public String name() { return "local-fake"; }
            public List<DeviceLease> discover(Platform platform) {
                return List.of(new DeviceLease("emulator-5554", 8200, "local-fake"));
            }
        };
        DevicePool pool = DevicePools.fromProvider(provider, Platform.ANDROID);
        DeviceLease lease = pool.acquire(5, TimeUnit.SECONDS);

        // driver (mock WebDriver that also takes screenshots)
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
        WebElement usernameField = mock(WebElement.class);
        when(driver.findElement(By.id("username"))).thenReturn(usernameField);
        when(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES))
                .thenReturn("png".getBytes(StandardCharsets.UTF_8));

        ContextManager.set(DriverContext.builder().driver(driver).device(lease).build());
        try {
            // observability wired to the bus
            Path captureDir = new CaptureLayout(work.resolve("reports"))
                    .testDir("run1", lease.deviceId(), "LoginSmoke");
            EventBus bus = new EventBus();
            ScreenshotProvider screenshots = () ->
                    ContextManager.current().driver() instanceof TakesScreenshot ts
                            ? Optional.of(ts) : Optional.empty();
            bus.subscribe(new CaptureListener(
                    new Capture(Capture.When.ON_ACTION, Capture.When.OFF, false, false),
                    captureDir, new Screenshotter(), screenshots, new SplitLogWriter(captureDir)));
            bus.emit(TestEvent.TEST_START, Map.of("device", lease.deviceId()));

            // locators (from YAML) + smart-find + heuristic heal + actions, via the page object
            LocatorRepository repo;
            try (InputStream in = getClass().getResourceAsStream("/locators.yaml")) {
                repo = LocatorRepository.fromYaml(in);
            }
            SmartFinder finder = new SmartFinder(repo, new LocatorStats(), new HeuristicElementHealer());
            LoginScreen login = new LoginScreen(finder, new ElementActions());

            login.enterUsername("alice");
            bus.emit(TestEvent.AFTER_ACTION, Map.of("name", "type"));

            // reporting
            Path reportDir = work.resolve("report");
            new Reporters(List.of(new SimpleHtmlReporter()))
                    .report(List.of(TestResult.pass("LoginSmoke", 42, lease.deviceId())), reportDir);

            // assertions across the whole flow
            verify(usernameField).sendKeys("alice");
            assertThat(Files.list(captureDir).anyMatch(p -> p.toString().endsWith(".png"))).isTrue();
            assertThat(Files.readString(captureDir.resolve("test.log"))).contains("AFTER_ACTION");
            assertThat(Files.readString(reportDir.resolve("report.html"))).contains("LoginSmoke");
        } finally {
            ContextManager.clear();
            pool.release(lease);
        }
    }
}
