package com.oxywatt.diagnostic

import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.FitEffect
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState

class DiagnosticExtension : KarooExtension("oxywatt-diagnostic", BuildConfig.VERSION_NAME) {
    private lateinit var system: KarooSystemService

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "extension created")
        system = KarooSystemService(this)
        system.addConsumer<RideState>(onError = { Log.w(TAG, "ride state subscription failed") }) {
            val state =
                when (it) {
                    RideState.Idle -> "IDLE"
                    RideState.Recording -> "RECORDING"
                    is RideState.Paused -> "PAUSED auto=${it.auto}"
                }
            Log.i(TAG, "ride=$state receiptMs=${SystemClock.elapsedRealtime()}")
        }
        observe("power", DataType.Type.POWER, DataType.Field.POWER)
        observe("heart-rate", DataType.Type.HEART_RATE, DataType.Field.HEART_RATE, positive = true)
        observe("cadence", DataType.Type.CADENCE, DataType.Field.CADENCE)
        system.connect { connected -> Log.i(TAG, "karoo connected=$connected") }
    }

    override fun startFit(emitter: Emitter<FitEffect>) {
        emitter.setCancellable { Log.i(TAG, "fit binding stopped") }
        Log.i(TAG, "fit binding started")
    }

    override fun onDestroy() {
        try {
            // The SDK disconnect removes all consumers registered by this service instance.
            system.disconnect()
        } catch (_: IllegalArgumentException) {
            Log.w(TAG, "Karoo service already unbound")
        } catch (_: RemoteException) {
            Log.w(TAG, "Karoo service disconnected during teardown")
        } finally {
            Log.i(TAG, "extension destroyed")
            super.onDestroy()
        }
    }

    private fun observe(label: String, type: String, field: String, positive: Boolean = false) {
        val probe = StreamProbe(label, field, positive)
        system.addConsumer<OnStreamState>(
            OnStreamState.StartStreaming(type),
            onError = { Log.w(TAG, "$label subscription failed") },
            onComplete = { Log.i(TAG, "$label subscription completed") },
        ) {
            probe.record(it.state)
        }
    }

    private class StreamProbe(
        private val label: String,
        private val field: String,
        private val positive: Boolean,
    ) {
        private var previousReceipt: Long? = null
        private var previousLog: Long? = null
        private var previousStatus: String? = null

        @Synchronized
        fun record(state: StreamState) {
            val receipt = SystemClock.elapsedRealtime()
            val status =
                when (state) {
                    StreamState.Idle -> "idle"
                    StreamState.Searching -> "searching"
                    StreamState.NotAvailable -> "unavailable"
                    is StreamState.Streaming -> {
                        val value = state.dataPoint.values[field]
                        when {
                            value == null -> "field-missing"
                            !value.isFinite() || value < 0 || (positive && value == 0.0) ->
                                "invalid"
                            else -> "present"
                        }
                    }
                }
            val interval = previousReceipt?.let { receipt - it }
            previousReceipt = receipt
            if (status != previousStatus || previousLog?.let { receipt - it >= 5000 } != false) {
                Log.i(TAG, "$label=$status receiptMs=$receipt previousCallbackMs=$interval")
                previousStatus = status
                previousLog = receipt
            }
        }
    }

    private companion object {
        const val TAG = "OxyWattProbe"
    }
}
