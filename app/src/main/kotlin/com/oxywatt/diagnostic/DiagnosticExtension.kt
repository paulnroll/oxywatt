package com.oxywatt.diagnostic

import android.os.SystemClock
import android.util.Log
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.FitEffect
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState

class DiagnosticExtension : KarooExtension("oxywatt-diagnostic", BuildConfig.VERSION_NAME) {
    private val savedCurveField = SavedCurveDataType()
    override val types: List<io.hammerhead.karooext.extension.DataTypeImpl> =
        listOf(savedCurveField)
    private lateinit var sourceHandle: AutoCloseable
    private lateinit var capture: DiagnosticCapture

    override fun onCreate() {
        super.onCreate()
        capture = DiagnosticRuntime.capture(applicationContext)
        Log.i(TAG, "extension created")
        val probes =
            mapOf(
                "power" to StreamProbe("power", DataType.Field.POWER, false),
                "heart-rate" to StreamProbe("heart-rate", DataType.Field.HEART_RATE, true),
                "cadence" to StreamProbe("cadence", DataType.Field.CADENCE, false),
            )
        sourceHandle =
            DiagnosticRuntime.startLive(applicationContext) { coordinator, capture, origin, signal
                ->
                capture.sourceStarted(origin)
                var wasConnected = false
                KarooRideDataSource(
                        KarooSystemService(this),
                        stamp = coordinator::stamp,
                        deliveryGate = coordinator.gate,
                        onRide = {
                            val state =
                                when (it) {
                                    RideState.Idle -> "IDLE"
                                    RideState.Recording -> "RECORDING"
                                    is RideState.Paused -> "PAUSED auto=${it.auto}"
                                }
                            Log.i(TAG, "ride=$state receiptMs=${SystemClock.elapsedRealtime()}")
                        },
                        onStream = { label, state -> probes.getValue(label).record(state) },
                        onConnection = {
                            capture.connection(it)
                            if (wasConnected && !it) {
                                coordinator.stopped(
                                    com.oxywatt.core.collection.InterruptionReason.TRANSPORT_LOSS
                                )
                                signal()
                            }
                            if (it) wasConnected = true
                            Log.i(TAG, "karoo connected=$it")
                        },
                    )
                    .start(
                        accept = {
                            capture.accept(it)
                            coordinator.accept(it)
                            signal()
                        },
                        onStopped = {
                            capture.sourceStopped(it != null)
                            coordinator.stopped(
                                if (it != null)
                                    com.oxywatt.core.collection.InterruptionReason.SOURCE_FAILED
                                else com.oxywatt.core.collection.InterruptionReason.SOURCE_STOPPED
                            )
                            signal()
                            if (it != null) Log.w(TAG, "Karoo source stopped with error")
                        },
                    )
            }
    }

    override fun startFit(emitter: Emitter<FitEffect>) {
        capture.fitBinding(true)
        emitter.setCancellable {
            capture.fitBinding(false)
            Log.i(TAG, "fit binding stopped")
        }
        Log.i(TAG, "fit binding started")
    }

    override fun onDestroy() {
        try {
            savedCurveField.close()
            sourceHandle.close()
        } finally {
            Log.i(TAG, "extension destroyed")
            super.onDestroy()
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
