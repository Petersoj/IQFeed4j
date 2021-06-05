package net.jacobpeterson.iqfeed4j.feed.streaming.derivative;

import net.jacobpeterson.iqfeed4j.feed.message.FeedMessageListener;
import net.jacobpeterson.iqfeed4j.model.feed.streaming.derivative.Interval;

/**
 * {@link IntervalListener} is a {@link FeedMessageListener} for {@link Interval}s.
 */
public abstract class IntervalListener implements FeedMessageListener<Interval> {
}
