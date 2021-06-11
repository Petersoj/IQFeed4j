package net.jacobpeterson.iqfeed4j.properties;

import net.jacobpeterson.iqfeed4j.IQFeed4j;
import net.jacobpeterson.iqfeed4j.util.reflection.ReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static net.jacobpeterson.iqfeed4j.util.properties.PropertyUtil.getProperty;

/**
 * {@link IQFeed4jProperties} defines properties for {@link IQFeed4j}.
 */
public class IQFeed4jProperties {

    /**
     * The properties file name.
     */
    public static final String PROPERTIES_FILE = "iqfeed4j.properties";
    /**
     * The default properties file name.
     */
    public static final String DEFAULT_PROPERTIES_FILE = "iqfeed4j.default.properties";

    private static final Logger LOGGER = LoggerFactory.getLogger(IQFeed4jProperties.class);

    private static final String IQCONNECT_COMMAND_KEY = "iqconnect_command";
    /** The value of {@link #IQCONNECT_COMMAND_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String IQCONNECT_COMMAND = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            IQCONNECT_COMMAND_KEY);

    private static final String PRODUCT_ID_KEY = "product_id";
    /** The value of {@link #PRODUCT_ID_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String PRODUCT_ID = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            PRODUCT_ID_KEY);

    private static final String APPLICATION_VERSION_KEY = "application_version";
    /** The value of {@link #APPLICATION_VERSION_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String APPLICATION_VERSION = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            APPLICATION_VERSION_KEY);

    private static final String LOGIN_KEY = "login";
    /** The value of {@link #LOGIN_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String LOGIN = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            LOGIN_KEY);

    private static final String PASSWORD_KEY = "password";
    /** The value of {@link #PASSWORD_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String PASSWORD = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            PASSWORD_KEY);

    private static final String AUTOCONNECT_KEY = "autoconnect";
    /** The value of {@link #AUTOCONNECT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String AUTOCONNECT = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            AUTOCONNECT_KEY);

    private static final String SAVE_LOGIN_INFO_KEY = "save_login_info";
    /** The value of {@link #SAVE_LOGIN_INFO_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String SAVE_LOGIN_INFO = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            SAVE_LOGIN_INFO_KEY);

    private static final String FEED_NAME_KEY = "feed_name";
    /** The value of {@link #FEED_NAME_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String FEED_NAME = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            FEED_NAME_KEY);

    private static final String FEED_HOSTNAME_KEY = "feed_hostname";
    /** The value of {@link #FEED_HOSTNAME_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String FEED_HOSTNAME = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            FEED_HOSTNAME_KEY);

    private static final String LEVEL_1_FEED_PORT_KEY = "level_1_feed_port";
    /** The value of {@link #LEVEL_1_FEED_PORT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String LEVEL_1_FEED_PORT = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            LEVEL_1_FEED_PORT_KEY);

    private static final String MARKET_DEPTH_FEED_PORT_KEY = "market_depth_feed_port";
    /** The value of {@link #MARKET_DEPTH_FEED_PORT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String MARKET_DEPTH_FEED_PORT = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            MARKET_DEPTH_FEED_PORT_KEY);

    private static final String DERIVATIVE_FEED_PORT_KEY = "derivative_feed_port";
    /** The value of {@link #DERIVATIVE_FEED_PORT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String DERIVATIVE_FEED_PORT = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            DERIVATIVE_FEED_PORT_KEY);

    private static final String ADMIN_FEED_PORT_KEY = "admin_feed_port";
    /** The value of {@link #ADMIN_FEED_PORT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String ADMIN_FEED_PORT = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            ADMIN_FEED_PORT_KEY);

    private static final String LOOKUP_FEED_PORT_KEY = "lookup_feed_port";
    /** The value of {@link #LOOKUP_FEED_PORT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String LOOKUP_FEED_PORT = getProperty(PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            LOOKUP_FEED_PORT_KEY);

    /**
     * Static {@link #toString()}.
     *
     * @return the static {@link #toString()}
     */
    public static String staticToString() {
        return IQFeed4jProperties.class.getName() + "[" +
                ReflectionUtil.getAllDeclaredFields(IQFeed4jProperties.class).stream()
                        .filter(field -> !field.getName().contains("LOGGER"))
                        .map(field -> {
                            try {
                                return field.getName() + " = " + field.get(null);
                            } catch (IllegalAccessException exception) {
                                LOGGER.error("Could not get field!", exception);
                            }

                            return field.getName() + " = ?";
                        }).collect(Collectors.joining(", "))
                + "]";
    }
}
