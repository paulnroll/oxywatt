package com.oxywatt.core.collection

/** Transport observation before Collector-owned session identity and admission. */
data class SourceObservation(
    val stamp: ReceiptStamp,
    val source: SourceRef,
    val normalizationVersion: String,
    val payload: InputPayload,
) {
    init {
        require(normalizationVersion.isNotBlank())
    }
}

/**
 * Single-use source. Callbacks are serialized. close requests cancellation; onStopped acknowledges
 * quiescence after the last accept exits and resources are released. EOF/cancellation report null;
 * delivery/cleanup failure reports its error. No terminal outcome manufactures a recording state.
 * Cancellation inside accept must not wait on that callback. Admission must be bounded by the sink.
 */
fun interface RideDataSource {
    fun start(accept: (SourceObservation) -> Unit, onStopped: (Throwable?) -> Unit): AutoCloseable
}
