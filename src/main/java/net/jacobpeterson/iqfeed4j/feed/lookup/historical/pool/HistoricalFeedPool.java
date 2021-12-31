package net.jacobpeterson.iqfeed4j.feed.lookup.historical.pool;

import net.jacobpeterson.iqfeed4j.feed.lookup.historical.HistoricalFeed;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * {@link HistoricalFeedPool} contains a thread-safe pool of {@link HistoricalFeed}s and has IQFeed's history request
 * rate limit built in. This class is designed to allow a user to make as many simultaneous {@link HistoricalFeed}
 * requests that IQFeed allows.
 */
public class HistoricalFeedPool {

    /**
     * Defines the feed request time delay in milliseconds which is currently 20 ms (50 requests/second) plus 1 ms for
     * margin.
     */
    public static final int DEFAULT_FEED_REQUEST_TIME_DELAY_MILLIS = 20 + 1;

    private final ObjectPool<HistoricalFeed> pool;
    private final Object lastRequestMillisLock;
    private long lastRequestMillis;

    /**
     * Instantiates a new {@link HistoricalFeedPool} using {@link Factory} as the {@link HistoricalFeed} {@link
     * PooledObjectFactory} and a customized {@link GenericObjectPoolConfig}.
     *
     * @param historicalFeedName the {@link HistoricalFeed} name
     * @param hostname           the hostname
     * @param port               the port
     */
    public HistoricalFeedPool(String historicalFeedName, String hostname, int port) {
        final GenericObjectPoolConfig<HistoricalFeed> feedPoolConfig = new GenericObjectPoolConfig<>();
        // Configure max/min pool objects
        feedPoolConfig.setMaxTotal(100);
        feedPoolConfig.setMaxIdle(-1);
        feedPoolConfig.setMinIdle(0);
        feedPoolConfig.setBlockWhenExhausted(true);
        // Test the feed instance to validate it before use
        feedPoolConfig.setTestOnBorrow(true);
        feedPoolConfig.setTestOnCreate(true);
        feedPoolConfig.setTestOnReturn(true);
        // Configure eviction of idle feeds
        feedPoolConfig.setEvictionPolicy(new DefaultEvictionPolicy<>());
        feedPoolConfig.setMinEvictableIdleTime(Duration.ofSeconds(30));
        feedPoolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(25));
        feedPoolConfig.setNumTestsPerEvictionRun(-3); // Evict 1/3rd of idle feeds

        pool = new GenericObjectPool<>(new Factory(historicalFeedName, hostname, port), feedPoolConfig);
        lastRequestMillisLock = new Object();
    }

    /**
     * Instantiates a new {@link HistoricalFeedPool} using {@link Factory} as the {@link HistoricalFeed} {@link
     * PooledObjectFactory} and a given {@link GenericObjectPoolConfig}.
     *
     * @param historicalFeedName the {@link HistoricalFeed} name
     * @param hostname           the hostname
     * @param port               the port
     * @param feedPoolConfig     the {@link HistoricalFeed} {@link GenericObjectPoolConfig}
     */
    public HistoricalFeedPool(String historicalFeedName, String hostname, int port,
            GenericObjectPoolConfig<HistoricalFeed> feedPoolConfig) {
        pool = new GenericObjectPool<>(new Factory(historicalFeedName, hostname, port), feedPoolConfig);
        lastRequestMillisLock = new Object();
    }

    /**
     * Instantiates a new {@link HistoricalFeedPool}.
     *
     * @param objectPool the {@link ObjectPool} of {@link HistoricalFeed}s to use
     */
    public HistoricalFeedPool(ObjectPool<HistoricalFeed> objectPool) {
        checkArgument(objectPool.getNumActive() <= 0);
        this.pool = objectPool;
        lastRequestMillisLock = new Object();
    }

    /**
     * Stops this instance of {@link HistoricalFeedPool} (stops/closes all {@link HistoricalFeed}s in the feed pool).
     */
    public void stop() {
        pool.close();
    }

    /**
     * Synchronously makes a request to a {@link HistoricalFeed} in this {@link HistoricalFeedPool} given a request
     * {@link Consumer}. This method may block for up to {@link Factory#getFeedRequestTimeDelayMillis()} milliseconds.
     *
     * @param historicalFeedConsumer the {@link HistoricalFeed} {@link Consumer}
     *
     * @throws Exception thrown for a variety of {@link Exception}s
     */
    public void request(Consumer<HistoricalFeed> historicalFeedConsumer) throws Exception {
        blockIfRateLimited();

        HistoricalFeed borrowedHistoricalFeed = pool.borrowObject();
        try {
            historicalFeedConsumer.accept(borrowedHistoricalFeed);
        } finally {
            pool.returnObject(borrowedHistoricalFeed);
        }
    }

    /**
     * Blocks until time since {@link #lastRequestMillis} is greater than {@link #getFeedRequestTimeDelayMillis()}. Also
     * sets {@link #lastRequestMillis} to the current time.
     *
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    private void blockIfRateLimited() throws InterruptedException {
        synchronized (lastRequestMillisLock) {
            long elapsedMillis = System.currentTimeMillis() - lastRequestMillis;
            if (elapsedMillis < getFeedRequestTimeDelayMillis()) {
                Thread.sleep(getFeedRequestTimeDelayMillis() - elapsedMillis);
            }

            lastRequestMillis = System.currentTimeMillis();
        }
    }

    /**
     * Gets the time delay in milliseconds for feed request rate limiting.
     *
     * @return a positive number of milliseconds
     */
    protected long getFeedRequestTimeDelayMillis() {
        return DEFAULT_FEED_REQUEST_TIME_DELAY_MILLIS;
    }

    /**
     * {@link Factory} is a {@link PooledObjectFactory} for {@link HistoricalFeed}s.
     */
    public static class Factory implements PooledObjectFactory<HistoricalFeed> {

        private final String historicalFeedName;
        private final String hostname;
        private final int port;

        /**
         * Instantiates a new {@link Factory}.
         *
         * @param historicalFeedName the {@link HistoricalFeed} name
         * @param hostname           the hostname
         * @param port               the port
         */
        public Factory(String historicalFeedName, String hostname, int port) {
            this.historicalFeedName = historicalFeedName;
            this.hostname = hostname;
            this.port = port;
        }

        @Override
        public PooledObject<HistoricalFeed> makeObject() throws Exception {
            HistoricalFeed historicalFeed = new HistoricalFeed(historicalFeedName, hostname, port);
            historicalFeed.start();
            historicalFeed.waitForProtocolVersionValidation(15, TimeUnit.SECONDS);

            return new DefaultPooledObject<>(historicalFeed);
        }

        @Override
        public void activateObject(PooledObject<HistoricalFeed> pooledObject) {}

        @Override
        public void passivateObject(PooledObject<HistoricalFeed> pooledObject) {}

        @Override
        public void destroyObject(PooledObject<HistoricalFeed> pooledObject) throws Exception {
            pooledObject.getObject().stop();
        }

        @Override
        public boolean validateObject(PooledObject<HistoricalFeed> pooledObject) {
            return pooledObject.getObject().isValid();
        }
    }
}
