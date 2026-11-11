package com.oxywatt.diagnostic.preview;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;

/** Device regression: exercise bundled FIT parsing on Android's actual java.time runtime. */
public class PreviewReplayInstrumentation extends Instrumentation {
    private boolean lifecycle;

    @Override
    public void onCreate(Bundle arguments) {
        lifecycle = arguments != null && "lifecycle".equals(arguments.getString("scenario"));
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        try {
            if (lifecycle) {
                verifyActivityLifecycle();
                result.putString("stream", "PASS: Activity recreation and reopen retain one demo source\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            if (!PreviewRun.INSTANCE.start(getTargetContext(), true, false)) {
                throw new AssertionError("Preview worker was already running");
            }
            long deadline = System.nanoTime() + 15_000_000_000L;
            String snapshot = "";
            while (System.nanoTime() < deadline) {
                Thread.sleep(50);
                snapshot = PreviewRun.INSTANCE.getSnapshot();
                if (snapshot != null && snapshot.contains("failure:")) {
                    throw new AssertionError(snapshot);
                }
                if (snapshot != null && snapshot.contains("EOF / stopped")) break;
            }
            if (snapshot == null || !snapshot.contains("Saved canonical events: 15")
                    || !snapshot.contains("Committed session: INTERRUPTED")) {
                throw new AssertionError("Unexpected terminal replay: " + snapshot);
            }
            result.putString("stream", "PASS: bundled FIT commits 15 events and remains interrupted\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", "FAIL: " + failure + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private void verifyActivityLifecycle() throws Exception {
        if (!PreviewRun.INSTANCE.start(getTargetContext(), false, false)) {
            throw new AssertionError("Preview worker was already running");
        }
        Intent intent = new Intent(getTargetContext(), PreviewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity original = startActivitySync(intent);
        ActivityMonitor monitor = addMonitor(PreviewActivity.class.getName(), null, false);
        Thread.sleep(1000);
        runOnMainSync(original::recreate);
        Activity recreated = waitForMonitorWithTimeout(monitor, 3000);
        removeMonitor(monitor);
        if (recreated == null || recreated == original) {
            throw new AssertionError("Activity recreation was not observed");
        }
        if (PreviewRun.INSTANCE.start(getTargetContext(), false, false)) {
            throw new AssertionError("Recreation allowed a duplicate source");
        }
        runOnMainSync(recreated::finish);
        waitForIdleSync();
        startActivitySync(intent);
        if (PreviewRun.INSTANCE.start(getTargetContext(), false, false)) {
            throw new AssertionError("Reopen allowed a duplicate source");
        }
        long deadline = System.nanoTime() + 12_000_000_000L;
        while (System.nanoTime() < deadline) {
            String state = PreviewRun.INSTANCE.getSnapshot();
            if (state != null && state.contains("EOF / stopped")) {
                if (!state.contains("Saved canonical events: 7")) {
                    throw new AssertionError("Demo event count changed: " + state);
                }
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Demo did not finish");
    }
}
