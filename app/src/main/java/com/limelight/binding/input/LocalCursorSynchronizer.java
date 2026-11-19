package com.limelight.binding.input;

/**
 * Owns all connection-specific state required to synchronise the client-side
 * (local) cursor position with the remote host:
 *
 * <ul>
 *   <li>The single authoritative connection-generation counter. Pending delayed
 *       callbacks from older generations are always ignored.</li>
 *   <li>The single authoritative host-synchronisation completion state. A valid
 *       {@link LocalCursorState} position does NOT imply the current connection
 *       has received it; completion is only reached once an absolute position
 *       has actually been submitted to the active connection (completion is
 *       defined as successful submission, as the protocol provides no
 *       acknowledgement).</li>
 *   <li>The bounded retry chain: one immediate attempt issued by
 *       {@link #requestSync()} followed by at most {@value #MAX_SYNC_RETRIES}
 *       delayed retries spaced {@value #SYNC_RETRY_DELAY_MS} ms apart. Only one
 *       retry chain can ever be pending at a time.</li>
 * </ul>
 *
 * <p>This class has no Android dependencies. The scheduler, stream dimension
 * source, host position sender and host condition probes are injected so the
 * full retry/lifecycle state machine can be unit tested with fakes.</p>
 */
public final class LocalCursorSynchronizer {

    public static final int MAX_SYNC_RETRIES = 20;
    public static final long SYNC_RETRY_DELAY_MS = 250;

    /** Abstraction over a delayed task scheduler (e.g. an Android Handler). */
    public interface Scheduler {
        void postDelayed(Runnable runnable, long delayMs);
        void removeCallbacks(Runnable runnable);
    }

    /** Provides the current (never cached) stream container dimensions. */
    public interface DimensionSource {
        int getWidth();
        int getHeight();
    }

    /** Submits an absolute cursor position to the host. */
    public interface PositionSender {
        void sendMousePosition(short x, short y, short referenceWidth, short referenceHeight);
    }

    /** Probes the current host/connection and local cursor state. */
    public interface HostState {
        boolean isLocalCursorEnabled();
        boolean isConnectionActive();
    }

    private final LocalCursorState cursorState;
    private final DimensionSource dimensionSource;
    private final PositionSender positionSender;
    private final Scheduler scheduler;
    private final HostState hostState;

    private int generation;
    private boolean syncCompleted;
    private int retriesUsed;
    private Runnable pendingRetry;

    public LocalCursorSynchronizer(LocalCursorState cursorState,
                                   DimensionSource dimensionSource,
                                   PositionSender positionSender,
                                   Scheduler scheduler,
                                   HostState hostState) {
        this.cursorState = cursorState;
        this.dimensionSource = dimensionSource;
        this.positionSender = positionSender;
        this.scheduler = scheduler;
        this.hostState = hostState;
    }

    private final class SyncRetryRunnable implements Runnable {
        private final int retryGeneration;

        SyncRetryRunnable(int retryGeneration) {
            this.retryGeneration = retryGeneration;
        }

        @Override
        public void run() {
            // A callback from a stale connection or a synchronisation that has
            // already completed (possibly via another input path) must not touch
            // any connection state.
            if (retryGeneration != generation || syncCompleted) {
                pendingRetry = null;
                return;
            }

            if (!hostState.isLocalCursorEnabled() || !hostState.isConnectionActive()) {
                pendingRetry = null;
                return;
            }

            if (retriesUsed >= MAX_SYNC_RETRIES) {
                pendingRetry = null;
                return;
            }

            retriesUsed++;

            if (attemptSync()) {
                pendingRetry = null;
                return;
            }

            // Never schedule an (MAX+1)-th delayed retry
            if (retriesUsed >= MAX_SYNC_RETRIES) {
                pendingRetry = null;
                return;
            }

            scheduleRetry(retryGeneration);
        }
    }

    /**
     * Marks the current connection as dead or superseded. Increments the
     * connection generation, cancels any pending delayed retry and resets the
     * completion state and retry budget. Must be called both when a new
     * connection starts and when a connection terminates.
     */
    public void invalidate() {
        generation++;
        cancelPendingRetry();
        syncCompleted = false;
        retriesUsed = 0;
    }

    /**
     * Performs the immediate host synchronisation attempt for the current
     * connection and, if it cannot complete yet (typically because the stream
     * container has not been measured), ensures a single bounded retry chain is
     * pending. Repeat calls never start a second chain and never reset the
     * delayed retry budget.
     *
     * @return true if the position was synchronised immediately.
     */
    public boolean requestSync() {
        if (attemptSync()) {
            return true;
        }

        if (!hostState.isLocalCursorEnabled() || !hostState.isConnectionActive()) {
            return false;
        }

        scheduleRetry(generation);
        return false;
    }

    /**
     * Records that a valid absolute cursor position has been submitted to the
     * active connection by another input path (relative mouse movement using
     * the local cursor, absolute physical pointer or absolute touchscreen
     * input). Cancels any pending retry chain immediately.
     */
    public void markCompleted() {
        syncCompleted = true;
        cancelPendingRetry();
    }

    /**
     * @return true if an absolute position has been submitted to the current
     *         active connection.
     */
    public boolean isSyncCompleted() {
        return syncCompleted;
    }

    /**
     * @return the current connection generation. All delayed callbacks capture
     *         the generation they were scheduled under and ignore themselves if
     *         it no longer matches.
     */
    public int getGeneration() {
        return generation;
    }

    // Visible for testing
    int getRetriesUsed() {
        return retriesUsed;
    }

    // Visible for testing
    boolean isRetryPending() {
        return pendingRetry != null;
    }

    private boolean attemptSync() {
        if (!hostState.isConnectionActive()) {
            return false;
        }

        int width = dimensionSource.getWidth();
        int height = dimensionSource.getHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }

        if (!cursorState.isPositionValid()) {
            cursorState.initializeCenter(width, height);
        } else {
            // Re-clamp a position retained from a previous connection or an
            // earlier window size against the current dimensions.
            cursorState.setPosition(cursorState.getX(), cursorState.getY(), width, height);
        }

        positionSender.sendMousePosition(
                cursorState.getShortX(),
                cursorState.getShortY(),
                (short) width,
                (short) height
        );

        syncCompleted = true;
        cancelPendingRetry();
        return true;
    }

    private void scheduleRetry(int retryGeneration) {
        // Re-posting replaces any already pending retry for this chain, so a
        // second requestSync() can never start a duplicate retry chain.
        cancelPendingRetry();

        SyncRetryRunnable retry = new SyncRetryRunnable(retryGeneration);
        pendingRetry = retry;
        scheduler.postDelayed(retry, SYNC_RETRY_DELAY_MS);
    }

    private void cancelPendingRetry() {
        if (pendingRetry != null) {
            scheduler.removeCallbacks(pendingRetry);
            pendingRetry = null;
        }
    }
}
