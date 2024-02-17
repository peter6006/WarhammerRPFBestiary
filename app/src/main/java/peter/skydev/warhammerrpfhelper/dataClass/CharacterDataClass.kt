package peter.skydev.warhammerrpfhelper.dataClass

data class CharacterDataClass(
    val name: String,
    val description: String,
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
    var skills: MutableList<String>,
    var talents: MutableList<String>
)