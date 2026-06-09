package io.framework.examples;

import io.framework.actions.ElementActions;
import io.framework.ai.llm.LlmClient;
import io.framework.core.config.FrameworkConfig;
import io.framework.core.context.ContextManager;
import io.framework.core.exec.SmartRetryAnalyzer;
import io.framework.core.failure.FailureClassifier;
import io.framework.core.driver.DriverProvider;
import io.framework.core.events.EventBus;
import io.framework.core.lifecycle.DriverLifecycle;
import io.framework.core.parallel.DevicePool;
import io.framework.core.spi.ServiceRegistry;
import io.framework.devices.DeviceProvider;
import io.framework.devices.DevicePools;
import io.framework.knowledge.KnowledgeStore;
import io.framework.locators.ElementHealer;
import io.framework.locators.LocatorRepository;
import io.framework.locators.SmartFinder;
import io.framework.observability.CaptureLayout;
import io.framework.observability.ScreenshotProvider;
import io.framework.observability.Screenshotter;
import io.framework.reporting.Reporter;
import io.framework.reporting.Reporters;
import org.openqa.selenium.TakesScreenshot;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Assembles the whole v1 stack from config + the locator repo, resolving swappable parts
 * (driver, devices, AI healer, reporters) through the ServiceRegistry / config. This is the
 * real entry point a runner uses; its mere compilation proves every module integrates. The
 * behaviour of the assembled flow is verified end-to-end (with fakes) in EndToEndSmokeTest.
 */
public final class Bootstrap {

    private Bootstrap() {
    }

    public static FrameworkComponents assemble(FrameworkConfig config,
                                               LocatorRepository locators,
                                               Path reportBase,
                                               Path knowledgeDir,
                                               LlmClient llmClient) {
        ServiceRegistry registry = new ServiceRegistry();

        DriverProvider driverProvider = registry.get(DriverProvider.class);
        DeviceProvider deviceProvider = registry.get(DeviceProvider.class);
        DevicePool pool = DevicePools.fromProvider(deviceProvider, config.platform());

        EventBus bus = new EventBus();
        DriverLifecycle lifecycle = new DriverLifecycle(pool, driverProvider, bus);

        // config-driven AI selection, wrapped in persistent memory (heuristic default, llm optional)
        KnowledgeStore knowledge = new KnowledgeStore(knowledgeDir);
        ElementHealer healer = AiSelection.elementHealer(config, knowledge, llmClient);
        FailureClassifier classifier = AiSelection.failureClassifier(config, knowledge, llmClient);
        SmartFinder finder = new SmartFinder(locators, knowledge.locators(), healer);
        ElementActions actions = new ElementActions();

        // activate classifier-driven retries from config
        SmartRetryAnalyzer.configure(config.retry(), classifier);

        Reporters reporters = new Reporters(registry.all(Reporter.class));
        CaptureLayout layout = new CaptureLayout(reportBase);
        Screenshotter screenshotter = new Screenshotter();
        ScreenshotProvider screenshotProvider = currentScreenshotSource();

        return new FrameworkComponents(bus, lifecycle, pool, finder, actions, classifier,
                reporters, layout, screenshotter, screenshotProvider);
    }

    private static ScreenshotProvider currentScreenshotSource() {
        return () -> {
            if (ContextManager.isSet()
                    && ContextManager.current().driver() instanceof TakesScreenshot shot) {
                return Optional.of(shot);
            }
            return Optional.empty();
        };
    }
}
