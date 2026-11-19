package com.limelight.binding.input;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Behaviour tests for the connection-aware host synchronisation and bounded
 * retry state machine of {@link LocalCursorSynchronizer}, driven entirely with
 * fake collaborators (no Android dependencies required).
 */
public class LocalCursorSynchronizerTest {

    private static class FakeScheduler implements LocalCursorSynchronizer.Scheduler {
        final List<Runnable> pending = new ArrayList<>();
        long lastDelayMs = -1;

        @Override
        public void postDelayed(Runnable runnable, long delayMs) {
            pending.add(runnable);
            lastDelayMs = delayMs;
        }

        @Override
        public void removeCallbacks(Runnable runnable) {
            pending.remove(runnable);
        }

        boolean runNextPending() {
            if (pending.isEmpty()) {
                return false;
            }
            pending.remove(0).run();
            return true;
        }
    }

    private static class FakeDimensions implements LocalCursorSynchronizer.DimensionSource {
        int width;
        int height;

        FakeDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    private static class FakeSender implements LocalCursorSynchronizer.PositionSender {
        final List<long[]> sent = new ArrayList<>();

        @Override
        public void sendMousePosition(short x, short y, short referenceWidth, short referenceHeight) {
            sent.add(new long[]{x, y, referenceWidth, referenceHeight});
        }
    }

    private static class FakeHostState implements LocalCursorSynchronizer.HostState {
        boolean enabled = true;
        boolean connected = true;

        @Override
        public boolean isLocalCursorEnabled() {
            return enabled;
        }

        @Override
        public boolean isConnectionActive() {
            return connected;
        }
    }

    private LocalCursorState state;
    private FakeDimensions dimensions;
    private FakeSender sender;
    private FakeScheduler scheduler;
    private FakeHostState hostState;
    private LocalCursorSynchronizer synchronizer;

    @Before
    public void setUp() {
        state = new LocalCursorState();
        state.setEnabled(true);
        dimensions = new FakeDimensions(1280, 720);
        sender = new FakeSender();
        scheduler = new FakeScheduler();
        hostState = new FakeHostState();
        synchronizer = new LocalCursorSynchronizer(state, dimensions, sender, scheduler, hostState);
    }

    @Test
    public void requestSync_withValidDimensions_sendsImmediatelyAndCompletes() {
        assertTrue(synchronizer.requestSync());

        assertEquals(1, sender.sent.size());
        // No valid position yet -> cursor is initialized at the video center
        assertArrayEquals(new long[]{640, 360, 1280, 720}, sender.sent.get(0));
        assertTrue(synchronizer.isSyncCompleted());
        assertFalse(synchronizer.isRetryPending());
        assertTrue(scheduler.pending.isEmpty());
    }

    @Test
    public void requestSync_withZeroDimensions_failsThenSucceedsAfterLayout() {
        dimensions.width = 0;
        dimensions.height = 0;

        assertFalse(synchronizer.requestSync());
        assertTrue(sender.sent.isEmpty());
        assertFalse(synchronizer.isSyncCompleted());
        assertTrue(synchronizer.isRetryPending());

        // The stream container is measured; the next retry must succeed
        dimensions.width = 1920;
        dimensions.height = 1080;

        assertEquals(LocalCursorSynchronizer.SYNC_RETRY_DELAY_MS, scheduler.lastDelayMs);
        assertTrue(scheduler.runNextPending());

        assertEquals(1, sender.sent.size());
        assertArrayEquals(new long[]{960, 540, 1920, 1080}, sender.sent.get(0));
        assertTrue(synchronizer.isSyncCompleted());
        assertFalse(synchronizer.isRetryPending());
        // No further retries are queued after success
        assertTrue(scheduler.pending.isEmpty());
    }

    @Test
    public void userMovementCompletesSync_stopsPendingRetry() {
        dimensions.width = 0;
        dimensions.height = 0;
        assertFalse(synchronizer.requestSync());
        assertTrue(synchronizer.isRetryPending());

        // The user actively moves the local cursor; that path submits its own
        // absolute position and reports completion.
        synchronizer.markCompleted();

        // The pending retry must have been cancelled immediately
        assertFalse(synchronizer.isRetryPending());
        assertTrue(scheduler.pending.isEmpty());
        assertTrue(synchronizer.isSyncCompleted());

        // Even if a stale callback somehow ran, it must not send anything
        dimensions.width = 1280;
        dimensions.height = 720;
        assertFalse(scheduler.runNextPending());
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    public void retryChain_isBoundedToMaximumRetries() {
        dimensions.width = 0; // never becomes valid
        assertFalse(synchronizer.requestSync());

        int executed = 0;
        while (scheduler.runNextPending()) {
            executed++;
            if (executed > 100) {
                fail("Retry chain never stopped");
            }
        }

        assertEquals(LocalCursorSynchronizer.MAX_SYNC_RETRIES, executed);
        assertEquals(0, sender.sent.size());
        assertFalse(synchronizer.isSyncCompleted());
    }

    @Test
    public void retryChain_singleChainOnly_noDuplicatesFromRepeatedRequests() {
        dimensions.width = 0;

        assertFalse(synchronizer.requestSync());
        assertFalse(synchronizer.requestSync());
        assertFalse(synchronizer.requestSync());

        // Repeated requests must not stack duplicate chains
        assertEquals(1, scheduler.pending.size());

        // And must not reset or grow the delayed retry budget
        assertEquals(0, synchronizer.getRetriesUsed());
    }

    @Test
    public void disableStopsRetryChain() {
        dimensions.width = 0;
        assertFalse(synchronizer.requestSync());

        hostState.enabled = false;
        assertTrue(scheduler.runNextPending());

        assertFalse(synchronizer.isRetryPending());
        assertTrue(scheduler.pending.isEmpty());
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    public void disconnectStopsRetryChain() {
        dimensions.width = 0;
        assertFalse(synchronizer.requestSync());

        hostState.connected = false;
        assertTrue(scheduler.runNextPending());

        assertFalse(synchronizer.isRetryPending());
        assertTrue(scheduler.pending.isEmpty());
    }

    @Test
    public void reconnection_resetsRetryBudgetAndCompletion() {
        dimensions.width = 0;
        assertFalse(synchronizer.requestSync());

        // Burn some of the retry budget on the first connection
        int burned = 0;
        while (burned < 5 && scheduler.runNextPending()) {
            burned++;
        }
        assertEquals(5, synchronizer.getRetriesUsed());

        int generationBefore = synchronizer.getGeneration();

        // New connection starts: state must be fully reset
        synchronizer.invalidate();
        assertEquals(generationBefore + 1, synchronizer.getGeneration());
        assertFalse(synchronizer.isSyncCompleted());
        assertEquals(0, synchronizer.getRetriesUsed());
        assertFalse(synchronizer.isRetryPending());
        assertTrue(scheduler.pending.isEmpty());

        // The fresh connection gets its full budget again and can complete
        dimensions.width = 1280;
        dimensions.height = 720;
        assertTrue(synchronizer.requestSync());
        assertEquals(1, sender.sent.size());
        assertTrue(synchronizer.isSyncCompleted());
    }

    @Test
    public void staleGenerationCallbacks_neverTouchNewConnectionState() {
        dimensions.width = 0;
        assertFalse(synchronizer.requestSync());

        // Capture the pending retry of the old connection even though a real
        // scheduler would have had it removed: it is executed manually here to
        // simulate a callback that slipped past cancellation.
        Runnable staleRetry = scheduler.pending.remove(0);

        // A new connection takes over
        synchronizer.invalidate();
        sender.sent.clear();

        // The stale callback must not send anything or re-arm retries
        staleRetry.run();
        assertTrue(sender.sent.isEmpty());
        assertFalse(synchronizer.isRetryPending());
        assertFalse(synchronizer.isSyncCompleted());
    }

    @Test
    public void reconnection_reusesAndReclampsExistingValidPosition() {
        // Valid position from a previous session outside the new bounds
        state.setPosition(1500f, 400f, 1920, 1080);
        synchronizer.invalidate();

        // New connection with a smaller viewport
        dimensions.width = 800;
        dimensions.height = 600;
        assertTrue(synchronizer.requestSync());

        assertEquals(1, sender.sent.size());
        assertArrayEquals(new long[]{800, 400, 800, 600}, sender.sent.get(0));
        assertEquals(800f, state.getX(), 0.01f);
        assertEquals(400f, state.getY(), 0.01f);
    }

    @Test
    public void markCompleted_cancelsPendingRetryImmediately() {
        dimensions.width = 0;
        assertFalse(synchronizer.requestSync());
        assertEquals(1, scheduler.pending.size());

        synchronizer.markCompleted();
        assertEquals(0, scheduler.pending.size());

        // markCompleted is idempotent and keeps completion sticky for the
        // current generation
        synchronizer.markCompleted();
        assertTrue(synchronizer.isSyncCompleted());
    }

    @Test
    public void stateTransitionAlone_neverSendsHostPackets() {
        // Enabling/disabling the cursor state is a pure local transition: the
        // synchronizer must not submit anything without a requestSync().
        state.setEnabled(false);
        state.setEnabledIfChanged(true);

        assertTrue(sender.sent.isEmpty());
        assertFalse(synchronizer.isSyncCompleted());
        assertTrue(scheduler.pending.isEmpty());
    }
}

