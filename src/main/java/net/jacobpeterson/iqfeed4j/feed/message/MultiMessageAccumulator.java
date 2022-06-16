package net.jacobpeterson.iqfeed4j.feed.message;

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
public class MultiMessageAccumulator<T> extends MultiMessageListener<T> {

    protected final CompletableFuture<Void> completionFuture;
    protected final List<T> messages;

    /**
     * Instantiates a new {@link MultiMessageAccumulator}.
     */
    public MultiMessageAccumulator() {
        completionFuture = new CompletableFuture<>();
        messages = new ArrayList<>();
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
     * Gets the {@link List} for all the messages in {@link #messages}. Note this will block until
     * {@link #onEndOfMultiMessage()} has been called by the underlying feed.
     *
     * @return an {@link List}
     *
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public List<T> getMessages() throws ExecutionException, InterruptedException {
        completionFuture.get();
        return messages;
    }

    /**
     * Calls {@link #getMessages()} {@link List#iterator()}.
     *
     * @return an {@link Iterator}
     *
     * @throws ExecutionException   thrown for {@link ExecutionException}s
     * @throws InterruptedException thrown for {@link InterruptedException}s
     */
    public Iterator<T> getIterator() throws ExecutionException, InterruptedException {
        return getMessages().iterator();
    }
}
