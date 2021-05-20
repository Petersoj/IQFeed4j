package net.jacobpeterson.iqfeed4j.feed;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * {@link SingleMessageFuture} is a {@link CompletableFuture} that represents a single message in the future.
 * <br>
 * Note that this {@link CompletableFuture} can {@link CompletableFuture#completeExceptionally(Throwable)} is there are
 * {@link Exception}s thrown when acquiring/converting the associated message.
 * <br>
 * Also note that any additional {@link CompletableFuture} chaining (e.g. calling {@link
 * CompletableFuture#thenApply(Function)} should never block the completion of this {@link SingleMessageFuture}!
 *
 * @param <T> the type of message
 */
public class SingleMessageFuture<T> extends CompletableFuture<T> {
}
