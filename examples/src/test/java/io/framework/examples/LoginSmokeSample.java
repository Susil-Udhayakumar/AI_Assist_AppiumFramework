package io.framework.examples;

import io.framework.core.base.BaseTest;
import io.framework.core.config.ConfigLoader;
import io.framework.core.config.FrameworkConfig;
import io.framework.core.config.Platform;
import io.framework.core.lifecycle.DriverLifecycle;
import io.framework.drivers.AppSource;
import io.framework.drivers.CapabilityBuilder;
import io.framework.locators.LocatorRepository;
import org.openqa.selenium.Capabilities;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * SAMPLE on-device test (TestNG) — not part of the automated build (filename is *Sample, not
 * *Test, so Surefire skips it). Run it against a real device with an Appium server up, e.g.:
 *
 *   mvn -pl examples test-compile
 *   mvn -pl examples surefire:test -Dtest=LoginSmokeSample \
 *       -Dappium.server.url=http://127.0.0.1:4723
 *
 * It shows the intended end-state authoring experience: config-driven, hybrid page object,
 * self-healing locators, automatic capture, no boilerplate in the test body.
 */
public class LoginSmokeSample extends BaseTest {

    private FrameworkComponents framework;

    @Override
    protected DriverLifecycle provideLifecycle() {
        return components().lifecycle();
    }

    @Override
    protected Platform providePlatform() {
        return Platform.ANDROID;
    }

    @Override
    protected Capabilities provideCapabilities() {
        return CapabilityBuilder.build(
                Platform.ANDROID, "emulator-5554", null,
                AppSource.installedAndroid("com.example.app", ".MainActivity"),
                Map.of());
    }

    @Test
    public void userCanLogIn() {
        LoginScreen login = new LoginScreen(components().finder(), components().elementActions());
        login.enterUsername("alice")
                .enterPassword("s3cr3t")
                .tapLogin();
        // ... assert the post-login screen here, e.g. a "Home" element is displayed
    }

    private FrameworkComponents components() {
        if (framework == null) {
            FrameworkConfig config = new ConfigLoader(
                    "config/staging.yaml", System::getProperty, System::getenv).load();
            LocatorRepository repo;
            try (InputStream in = getClass().getResourceAsStream("/locators.yaml")) {
                repo = LocatorRepository.fromYaml(in);
            } catch (Exception e) {
                throw new IllegalStateException("could not load locators.yaml", e);
            }
            framework = Bootstrap.assemble(config, repo, Path.of("reports"));
        }
        return framework;
    }
}
