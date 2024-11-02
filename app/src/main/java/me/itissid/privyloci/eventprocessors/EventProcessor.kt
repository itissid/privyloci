package me.itissid.privyloci.eventprocessors

import me.itissid.privyloci.datamodels.EventType

interface EventProcessor {
    fun startProcessing()
    fun stopProcessing()
}