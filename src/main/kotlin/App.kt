import kotlinx.serialization.Serializable

@Serializable
data class App(
    var bundle: String,
    var appName: String,
    var source: String? = null,
    var appsFlyer: String? = null,
    var fb: String? = null
)
