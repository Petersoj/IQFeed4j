package net.jacobpeterson.iqfeed4j.api.message;

import net.jacobpeterson.iqfeed4j.feed.message.MultiMessageListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * {@inheritDoc}
 * <br>
 * This will accumulate all data/messages in memory to be consumed later.
 */
public class AsyncMultiMessageListener<T> extends MultiMessageListener<T> {

    protected final CompletableFuture<Void> completionFuture;
    protected final List<T> messages;

    /**
     * Instantiates a new {@link AsyncMultiMessageListener}.
     */
    public AsyncMultiMessageListener() {
        completionFuture = new CompletableFuture<>();
        messages = new ArrayList<>(); // ArrayList provides best performance for Iterators
    }

    @Override
    public void onMessageReceived(T message) {
        messages.add(message);
    }

    @Override
    public void onMessageException(Exception exception) {
        completionFuture.completeExceptionally(exception);
    }

    @Override
    public void onEndOfMultiMessage() {
        completionFuture.complete(null);
    }

    /**
     * Gets an {@link Iterator} for all the messages in {@link #messages}. Note this will block until {@link
     * #onEndOfMultiMessage()}* has been called by the underlying feed.
     *
     * @return an {@link Iterator}
     *
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public Iterator<T> getIterator() throws ExecutionException, InterruptedException {
        completionFuture.get();
        return messages.iterator();
    }
}
