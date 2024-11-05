package me.itissid.privyloci.eventprocessors

import me.itissid.privyloci.datamodels.EventType

interface EventProcessor {
    fun startProcessing()
    fun stopProcessing()
}

class NoopEventProcessor : EventProcessor {
    override fun startProcessing() {
        // No-op
    }

    override fun stopProcessing() {
        // No-op
    }
}