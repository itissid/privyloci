package me.itissid.privyloci.datamodels

data class InternalBtDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val hasHighDefinitionAudioCapabilities: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is InternalBtDevice -> {
                return name == other.name && address == other.address
            }

            else -> false
        }
    }

    override fun hashCode(): Int {
        return 31 * name.hashCode() + address.hashCode()
    }

}