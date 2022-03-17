package com.nordstrom.automation.junit;

import java.io.IOException;

import org.apache.commons.configuration2.ex.ConfigurationException;
import com.nordstrom.automation.settings.SettingsCore;
import com.nordstrom.common.base.UncheckedThrow;

/**
 * This class declares the settings and methods related to JUnit configuration.
 * 
 * @see JUnitSettings
 */
public class JUnitConfig extends SettingsCore<JUnitConfig.JUnitSettings> {
    
    private static final String SETTINGS_FILE = "junit.properties";
    
    /**
     * This enumeration declares the settings that enable you to control the parameters
     * used by <b>JUnit Foundation</b>.
     * <p>
     * Each setting is defined by a constant name and System property key. Many settings
     * also define default values. Note that all of these settings can be overridden via
     * the {@code junit.properties} file and System property declarations.
     */
    public enum JUnitSettings implements SettingsCore.SettingsAPI {
        /**
         * This setting specifies the global per-test timeout interval in milliseconds.
         * <p>
         * name: <b>junit.timeout.test</b><br>
         * default: {@code null}
         */
        TEST_TIMEOUT("junit.timeout.test", null),
        
        /**
         * This setting specifies the global per-class timeout interval in milliseconds.
         * <p>
         * name: <b>junit.timeout.rule</b><br>
         * default: {@code null}
         */
        TIMEOUT_RULE("junit.timeout.rule", null),
        
        /**
         * This setting specifies the maximum retry attempts for failed test methods.
         * <p>
         * name: <b>junit.max.retry</b><br>
         * default: <b>0</b>
         */
        MAX_RETRY("junit.max.retry", "0"),

        /**
         * This setting specifies whether the exception that caused a test to fail will be logged in the notification
         * that the test is being retried.
         * <p>
         * name: <b>junit.retry.more.info</b><br>
         * default: {@code false}
         */
        RETRY_MORE_INFO("junit.retry.more.info", "false");
        
        private final String propertyName;
        private final String defaultValue;
        
        JUnitSettings(String propertyName, String defaultValue) {
            this.propertyName = propertyName;
            this.defaultValue = defaultValue;
        }
        
        @Override
        public String key() {
            return propertyName;
        }

        @Override
        public String val() {
            return defaultValue;
        }
    }
    
    private static final ThreadLocal<JUnitConfig> junitConfig = new InheritableThreadLocal<JUnitConfig>() {
        @Override
        protected JUnitConfig initialValue() {
            final ClassLoader original = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(LifecycleHooks.class.getClassLoader());
                return new JUnitConfig();
            } catch (ConfigurationException | IOException e) {
                throw UncheckedThrow.throwUnchecked(e);
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    };

    /**
     * Instantiate a <b>JUnit Foundation</b> configuration object.
     * 
     * @throws ConfigurationException If a failure is encountered while initializing this configuration object.
     * @throws IOException If a failure is encountered while reading from a configuration input stream.
     */
    public JUnitConfig() throws ConfigurationException, IOException {
        super(JUnitSettings.class);
    }

    /**
     * Get the JUnit configuration object for the specified context.
     * 
     * @return JUnit configuration object
     */
    public static JUnitConfig getConfig() {
        return junitConfig.get();
    }
    
    @Override
    public String getSettingsPath() {
        return SETTINGS_FILE;
    }
}
