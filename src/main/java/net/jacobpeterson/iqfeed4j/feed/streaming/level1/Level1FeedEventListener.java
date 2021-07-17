package net.jacobpeterson.iqfeed4j.feed.streaming.level1;

/**
 * {@link Level1FeedEventListener} is an arbitrary event listener for {@link Level1Feed}.
 * <br>
 * <strong>Calling methods in {@link Level1Feed} that send feed messages (which is most methods in that class) will
 * result in dead-lock! Use a separate thread as needed.</strong>
 */
public abstract class Level1FeedEventListener {

    /**
     * Called when the attempt to reconnect failed.
     */
    public abstract void onServerReconnectFailed();

    /**
     * Called when the attempt to watch a symbol has failed, due to the symbol limit being reached.
     *
     * @param symbol the symbol
     */
    public abstract void onSymbolLimitReached(String symbol);

    /**
     * Called when a request symbol was not found or the user is not authorized.
     *
     * @param symbol the symbol
     */
    public abstract void onSymbolNotWatched(String symbol);
}
