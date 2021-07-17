package net.jacobpeterson.iqfeed4j.properties;

import net.jacobpeterson.iqfeed4j.IQFeed4j;

import static net.jacobpeterson.iqfeed4j.util.properties.PropertyUtil.getProperty;

/**
 * {@link IQFeed4jProperties} defines properties for {@link IQFeed4j}.
 */
public class IQFeed4jProperties {

    /** The properties file name. */
    public static final String PROPERTIES_FILE = "iqfeed4j.properties";
    /** The default properties file name. */
    public static final String DEFAULT_PROPERTIES_FILE = "iqfeed4j.default.properties";

    private static final String IQCONNECT_COMMAND_KEY = "iqconnect_command";
    /** The value of {@link #IQCONNECT_COMMAND_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String IQCONNECT_COMMAND = getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            IQCONNECT_COMMAND_KEY);

    private static final String PRODUCT_ID_KEY = "product_id";
    /** The value of {@link #PRODUCT_ID_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String PRODUCT_ID = getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            PRODUCT_ID_KEY);

    private static final String APPLICATION_VERSION_KEY = "application_version";
    /** The value of {@link #APPLICATION_VERSION_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String APPLICATION_VERSION = getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            APPLICATION_VERSION_KEY);

    private static final String LOGIN_KEY = "login";
    /** The value of {@link #LOGIN_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String LOGIN = getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            LOGIN_KEY);

    private static final String PASSWORD_KEY = "password";
    /** The value of {@link #PASSWORD_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String PASSWORD = getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            PASSWORD_KEY);

    private static final String AUTOCONNECT_KEY = "autoconnect";
    /** The value of {@link #AUTOCONNECT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final Boolean AUTOCONNECT = Boolean.parseBoolean(getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            AUTOCONNECT_KEY));

    private static final String SAVE_LOGIN_INFO_KEY = "save_login_info";
    /** The value of {@link #SAVE_LOGIN_INFO_KEY} in {@link #PROPERTIES_FILE}. */
    public static final Boolean SAVE_LOGIN_INFO = Boolean.parseBoolean(getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            SAVE_LOGIN_INFO_KEY));

    private static final String FEED_NAME_KEY = "feed_name";
    /** The value of {@link #FEED_NAME_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String FEED_NAME = getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            FEED_NAME_KEY);

    private static final String FEED_HOSTNAME_KEY = "feed_hostname";
    /** The value of {@link #FEED_HOSTNAME_KEY} in {@link #PROPERTIES_FILE}. */
    public static final String FEED_HOSTNAME = getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            FEED_HOSTNAME_KEY);

    private static final String LEVEL_1_FEED_PORT_KEY = "level_1_feed_port";
    /** The value of {@link #LEVEL_1_FEED_PORT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final Integer LEVEL_1_FEED_PORT = Integer.parseInt(getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            LEVEL_1_FEED_PORT_KEY));

    private static final String MARKET_DEPTH_FEED_PORT_KEY = "market_depth_feed_port";
    /** The value of {@link #MARKET_DEPTH_FEED_PORT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final Integer MARKET_DEPTH_FEED_PORT = Integer.parseInt(getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            MARKET_DEPTH_FEED_PORT_KEY));

    private static final String DERIVATIVE_FEED_PORT_KEY = "derivative_feed_port";
    /** The value of {@link #DERIVATIVE_FEED_PORT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final Integer DERIVATIVE_FEED_PORT = Integer.parseInt(getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            DERIVATIVE_FEED_PORT_KEY));

    private static final String ADMIN_FEED_PORT_KEY = "admin_feed_port";
    /** The value of {@link #ADMIN_FEED_PORT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final Integer ADMIN_FEED_PORT = Integer.parseInt(getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            ADMIN_FEED_PORT_KEY));

    private static final String LOOKUP_FEED_PORT_KEY = "lookup_feed_port";
    /** The value of {@link #LOOKUP_FEED_PORT_KEY} in {@link #PROPERTIES_FILE}. */
    public static final Integer LOOKUP_FEED_PORT = Integer.parseInt(getProperty(
            PROPERTIES_FILE, DEFAULT_PROPERTIES_FILE,
            LOOKUP_FEED_PORT_KEY));
}
