package net.jacobpeterson.iqfeed4j.feed;

import java.util.concurrent.CompletableFuture;

/**
 * {@link SingleMessageFuture} is a {@link CompletableFuture} that represents a single message in the future.
 *
 * @param <T> the type of message
 */
public class SingleMessageFuture<T> extends CompletableFuture<T> {
}
