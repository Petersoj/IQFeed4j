package net.jacobpeterson.iqfeed4j.feed.streaming.derivative;

/**
 * {@link DerivativeFeedEventListener} is an event listener for {@link DerivativeFeed}.
 * <br>
 * NOTE: Calling methods in {@link DerivativeFeed} that send feed messages will result in dead-lock! Use a separate
 * thread as needed.
 */
public abstract class DerivativeFeedEventListener {

    /**
     * Called when a request symbol was not found or the user is not authorized.
     *
     * @param symbol the symbol
     */
    public abstract void onSymbolNotWatched(String symbol);

    /**
     * Called when the attempt to watch a symbol has failed, due to the symbol limit being reached.
     *
     * @param symbol the symbol
     */
    public abstract void onSymbolLimitReached(String symbol);

    /**
     * Called when the new interval bar watch has replaced the previous watched streaming interval bar watch.
     *
     * @param symbol    the symbol
     * @param requestID the request ID
     */
    public abstract void onReplacedPreviouslyWatchedSymbol(String symbol, String requestID);
}
