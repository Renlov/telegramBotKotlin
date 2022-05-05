import kotlinx.serialization.Serializable

@Serializable
data class App(
    val bundle: String,
    val appName: String,
    val source: String? = null,
    val appsFlyer: String? = null,
    val fb: String? = null
)
