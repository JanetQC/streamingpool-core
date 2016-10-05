package cern.streaming.pool.core.service.streamid.factory;

import cern.streaming.pool.core.service.StreamId;
import cern.streaming.pool.core.support.RxStreamSupport;
import cern.streaming.pool.core.testing.AbstractStreamTest;
import cern.streaming.pool.core.testing.subscriber.BlockingTestSubscriber;
import org.junit.Test;
import rx.Observable;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests for {@link CoreStreams}.
 *
 * @author timartin
 */
public class CoreStreamsTest extends AbstractStreamTest implements RxStreamSupport {

    @Test(expected = NullPointerException.class)
    public void testMappedStreamWithNullSourceStreamId() {
        CoreStreams.mappedStream(null, val -> Optional.of(val));
    }

    @Test(expected = NullPointerException.class)
    public void testMappedStreamWithNullConversionFunction() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>empty()).withUniqueStreamId();
        CoreStreams.mappedStream(sourceStreamId, null);
    }

    @Test
    public void testMappedStreamWithConversionThatAlwaysReturns() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>just(1, 3)).withUniqueStreamId();
        StreamId<Integer> mappedStreamId = CoreStreams.mappedStream(sourceStreamId, val -> Optional.of(val + 1));
        BlockingTestSubscriber<Integer> subscriber = createSubscriberAndWait(mappedStreamId);
        assertThat(subscriber.getValues()).hasSize(2).containsExactly(2, 4);
    }

    @Test
    public void testMappedStreamWithConversionThatDoesNotAlwaysReturns() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>just(1, 3)).withUniqueStreamId();
        StreamId<Integer> mappedStreamId = CoreStreams.mappedStream(sourceStreamId,
                val -> (val == 1) ? Optional.of(val) : Optional.empty());
        BlockingTestSubscriber<Integer> subscriber = createSubscriberAndWait(mappedStreamId);
        assertThat(subscriber.getValues()).hasSize(1).containsExactly(1);
    }

    @Test(expected = NullPointerException.class)
    public void testFlatMappedStreamWithNullSourceStreamId() {
        CoreStreams.flatMappedStream(null, Observable::just);
    }

    @Test(expected = NullPointerException.class)
    public void testFlatMappedStreamWithNullConversionFunction() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>empty()).withUniqueStreamId();
        CoreStreams.flatMappedStream(sourceStreamId, null);
    }

    @Test
    public void testFlatMappedStreamWithConversionAlwaysReturns() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>just(1, 3)).withUniqueStreamId();
        StreamId<Integer> flatMappedStreamId = CoreStreams.flatMappedStream(sourceStreamId,
                val -> Observable.just(val, val));
        BlockingTestSubscriber<Integer> subscriber = createSubscriberAndWait(flatMappedStreamId);
        assertThat(subscriber.getValues()).hasSize(4).containsExactly(1, 1, 3, 3);
    }

    @Test
    public void testFlatMappedStreamWithConversionThatDoesNotAlwaysReturns() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>just(1, 3)).withUniqueStreamId();
        StreamId<Integer> flatMappedStreamId = CoreStreams.flatMappedStream(sourceStreamId,
                val -> (val == 1) ? Observable.just(val, val) : Observable.empty());
        BlockingTestSubscriber<Integer> subscriber = createSubscriberAndWait(flatMappedStreamId);
        assertThat(subscriber.getValues()).hasSize(2).containsExactly(1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergedStreamWithNullSourceStreamIds() {
        CoreStreams.mergedStream(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergedStreamWithEmptySourceStreamIds() {
        CoreStreams.mergedStream(Collections.emptyList());
    }

    @Test
    public void testMergedStreamWithSingleSourceStreamId() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>just(1, 3)).withUniqueStreamId();
        StreamId<Integer> mergedStreamId = CoreStreams.mergedStream(Arrays.asList(sourceStreamId));
        BlockingTestSubscriber<Integer> subscriber = createSubscriberAndWait(mergedStreamId);
        assertThat(subscriber.getValues()).hasSize(2).containsExactly(1, 3);
    }

    @Test
    public void testMergedStreamWithMultipleSourceStreamIds() {
        StreamId<Integer> sourceStreamId1 = provide(Observable.<Integer>just(1, 3)).withUniqueStreamId();
        StreamId<Integer> sourceStreamId2 = provide(Observable.<Integer>just(2, 4)).withUniqueStreamId();
        StreamId<Integer> mergedStreamId = CoreStreams.mergedStream(Arrays.asList(sourceStreamId1, sourceStreamId2));
        BlockingTestSubscriber<Integer> subscriber = createSubscriberAndWait(mergedStreamId);
        assertThat(subscriber.getValues()).hasSize(4).contains(1, 2, 3, 4);
    }

    @Test(expected = NullPointerException.class)
    public void testFilteredStreamWithNullSourceStreamId() {
        CoreStreams.filteredStream(null, val -> true);
    }

    @Test(expected = NullPointerException.class)
    public void testFilteredStreamWithNullPredicate() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>empty()).withUniqueStreamId();
        CoreStreams.filteredStream(sourceStreamId, null);
    }

    @Test
    public void testFilteredStreamWithCorrectValues() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>just(1, 3)).withUniqueStreamId();
        StreamId<Integer> filteredStreamId = CoreStreams.filteredStream(sourceStreamId, val -> val == 1);
        BlockingTestSubscriber<Integer> subscriber = createSubscriberAndWait(filteredStreamId);
        assertThat(subscriber.getValues()).hasSize(1).contains(1);
    }

    @Test(expected = NullPointerException.class)
    public void testDelayedStreamWithNullSourceStreamId() {
        CoreStreams.delayedStream(null, Duration.ZERO);
    }

    @Test(expected = NullPointerException.class)
    public void testDelayedStreamWithNullDuration() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>empty()).withUniqueStreamId();
        CoreStreams.delayedStream(sourceStreamId, null);
    }

    @Test
    public void testDelayedStreamWithCorrectValues() {
        final long delay = 2000;
        final long deltaDelay = 500;
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>just(1)).withUniqueStreamId();
        StreamId<Integer> delayedStreamId = CoreStreams.delayedStream(sourceStreamId, Duration.ofMillis(delay));
        BlockingTestSubscriber<Integer> subscriber = BlockingTestSubscriber.ofName("subscriber");
        publisherFrom(delayedStreamId).subscribe(subscriber);

        Instant before = Instant.now();
        subscriber.await();
        Instant after = Instant.now();

        assertThat(subscriber.getValues()).hasSize(1).containsExactly(1);
        assertThat(Duration.between(before, after).toMillis()).isBetween(delay - deltaDelay, delay + deltaDelay);
    }

    @Test(expected = NullPointerException.class)
    public void testZippedStreamWithNullLeftStreamSourceId() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>empty()).withUniqueStreamId();
        CoreStreams.zippedStream(null, sourceStreamId, (val1, val2) -> Optional.empty());
    }

    @Test(expected = NullPointerException.class)
    public void testZippedStreamWithNullRightStreamSourceId() {
        StreamId<Integer> sourceStreamId = provide(Observable.<Integer>empty()).withUniqueStreamId();
        CoreStreams.zippedStream(sourceStreamId, null, (val1, val2) -> Optional.empty());
    }

    @Test(expected = NullPointerException.class)
    public void testZippedStreamWithNullZipFunction() {
        StreamId<Integer> sourceStreamId1 = provide(Observable.<Integer>empty()).withUniqueStreamId();
        StreamId<Integer> sourceStreamId2 = provide(Observable.<Integer>empty()).withUniqueStreamId();
        CoreStreams.zippedStream(sourceStreamId1, sourceStreamId2, null);
    }

    @Test
    public void testZippedStreamWithZipThatAlwaysReturns() {
        StreamId<Integer> sourceStreamId1 = provide(Observable.<Integer>just(1, 3)).withUniqueStreamId();
        StreamId<Integer> sourceStreamId2 = provide(Observable.<Integer>just(2, 4)).withUniqueStreamId();
        StreamId<Integer> zippedStreamId = CoreStreams.zippedStream(sourceStreamId1, sourceStreamId2,
                (val1, val2) -> Optional.of(val1 + val2));
        BlockingTestSubscriber<Integer> subscriber = createSubscriberAndWait(zippedStreamId);
        assertThat(subscriber.getValues()).hasSize(2).containsExactly(3, 7);
    }

    @Test
    public void testZippedStreamWithZipThatDoesNotAlwaysReturns() {
        StreamId<Integer> sourceStreamId1 = provide(Observable.<Integer>just(1, 3)).withUniqueStreamId();
        StreamId<Integer> sourceStreamId2 = provide(Observable.<Integer>just(2, 4)).withUniqueStreamId();
        StreamId<Integer> zippedStreamId = CoreStreams.zippedStream(sourceStreamId1, sourceStreamId2,
                (val1, val2) -> (val1 == 1) ? Optional.empty() : Optional.of(val1 + val2));
        BlockingTestSubscriber<Integer> subscriber = createSubscriberAndWait(zippedStreamId);
        assertThat(subscriber.getValues()).hasSize(1).containsExactly(7);
    }

    private final BlockingTestSubscriber<Integer> createSubscriberAndWait(StreamId<Integer> sourceStreamId) {
        BlockingTestSubscriber<Integer> subscriber = BlockingTestSubscriber.ofName("subscriber");
        publisherFrom(sourceStreamId).subscribe(subscriber);
        subscriber.await();
        return subscriber;
    }
}
