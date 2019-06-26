package net.serenitybdd.core.webdriver.driverproviders;

import com.google.common.base.Optional;
import net.serenitybdd.core.webdriver.servicepools.DriverServiceExecutable;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.webdriver.capabilities.AddCustomCapabilities;
import net.thucydides.core.webdriver.capabilities.ChromePreferences;
import net.thucydides.core.webdriver.chrome.OptionsSplitter;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.thucydides.core.ThucydidesSystemProperty.*;

public class ChromeDriverCapabilities implements DriverCapabilitiesProvider {

    private final static List<String> AUTOMATION_OPTIONS = Arrays.asList("--enable-automation","--test-type");
    private final EnvironmentVariables environmentVariables;
    private final String driverOptions;

    public ChromeDriverCapabilities(EnvironmentVariables environmentVariables, String driverOptions) {
        this.environmentVariables = environmentVariables;
        this.driverOptions = driverOptions;
    }

    @Override
    public DesiredCapabilities getCapabilities() {
        DesiredCapabilities capabilities = DesiredCapabilities.chrome();

        ChromeOptions chromeOptions = configuredOptions();

        capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);

        String chromeSwitches = environmentVariables.getProperty(ThucydidesSystemProperty.CHROME_SWITCHES);
        capabilities.setCapability("chrome.switches", chromeSwitches);

        AddCustomCapabilities.startingWith("chrome.capabilities.").from(environmentVariables).to(capabilities);

        SetProxyConfiguration.from(environmentVariables).in(capabilities);

        return capabilities;
    }

    private ChromeOptions configuredOptions() {
        ChromeOptions options = new ChromeOptions();

	/*
	 * This is the only way to set the Chrome _browser_ binary.
	 */
	if (WEBDRIVER_CHROME_BINARY.isDefinedIn(environmentVariables))
	    options.setBinary(WEBDRIVER_CHROME_BINARY.from(environmentVariables));

        addEnvironmentSwitchesTo(options);
        addRuntimeOptionsTo(options);
        addPreferencesTo(options);
        addExperimentalOptionsTo(options);
        updateChromeBinaryIfSpecified(options);

        return options;
    }

    private void addEnvironmentSwitchesTo(ChromeOptions options) {

        String chromeSwitches = environmentVariables.getProperty(ThucydidesSystemProperty.CHROME_SWITCHES);

        if (StringUtils.isNotEmpty(chromeSwitches)) {
            List<String> arguments = new OptionsSplitter().split(chromeSwitches);
            options.addArguments(arguments);
        }

        if (HEADLESS_MODE.isDefinedIn(environmentVariables) && HEADLESS_MODE.booleanFrom(environmentVariables, false)) {
            options.addArguments("--headless");
        }
    }

    private void addRuntimeOptionsTo(ChromeOptions options) {


        if (ThucydidesSystemProperty.USE_CHROME_AUTOMATION_OPTIONS.booleanFrom(environmentVariables,true)) {
            options.addArguments(AUTOMATION_OPTIONS);
        }

        if (StringUtils.isNotEmpty(driverOptions)) {
            List<String> arguments = new OptionsSplitter().split(driverOptions);
            options.addArguments(arguments);
        }



        options.setAcceptInsecureCerts(ACCEPT_INSECURE_CERTIFICATES.booleanFrom(environmentVariables, false));
    }

    private void addPreferencesTo(ChromeOptions options) {

        Map<String, Object> chromePreferences = ChromePreferences.startingWith("chrome_preferences.").from(environmentVariables);

        chromePreferences.putAll(ChromePreferences.startingWith("chrome.preferences.").from(environmentVariables));

        if (!chromePreferences.isEmpty()) {
            options.setExperimentalOption("prefs", chromePreferences);
        }
    }

    private void addExperimentalOptionsTo(ChromeOptions options) {

        Map<String, Object> chrome_experimental_options = ChromePreferences.startingWith("chrome_experimental_options.").from(environmentVariables);

        Map<String, Object> nestedExperimentalOptions = ChromePreferences.startingWith("chrome.experimental_options.").from(environmentVariables);
        chrome_experimental_options.putAll(nestedExperimentalOptions);

        chrome_experimental_options.keySet().forEach(
                key -> options.setExperimentalOption(key, chrome_experimental_options.get(key))
        );
    }

    private void updateChromeBinaryIfSpecified(ChromeOptions options) {

        File executable = DriverServiceExecutable.called("chromedriver")
                .withSystemProperty(WEBDRIVER_CHROME_DRIVER.getPropertyName())
                .usingEnvironmentVariables(environmentVariables)
                .reportMissingBinary()
                .downloadableFrom("https://sites.google.com/a/chromium.org/chromedriver/downloads")
                .asAFile();

        if (executable != null && executable.exists()) {
            System.setProperty("webdriver.chrome.driver", executable.getAbsolutePath());
        }
    }

}
