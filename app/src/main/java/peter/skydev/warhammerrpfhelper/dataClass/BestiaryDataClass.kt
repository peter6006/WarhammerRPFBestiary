package peter.skydev.warhammerrpfhelper.dataClass

data class BestiaryDataClass(
    val name: String,
    val image: String?,
    val M: Int,
    val WS: Int,
    val BS: Int,
    val S: Int,
    val T: Int,
    val I: Int,
    val Ag: Int,
    val Dex: Int,
    val Int: Int,
    val WP: Int,
    val Fel: Int,
    val W: Int,
    val mandatory_traits: MutableList<String>,
    val optional_traits: MutableList<String>
)