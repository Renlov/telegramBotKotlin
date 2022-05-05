import Networking.client
import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

suspend fun main() {
    val bot = telegramBot("bot token")
    bot.buildBehaviourWithLongPolling {
        println(getMe())

        onCommand("apps") {
            reply(it, "Ищу...")
            val apps = getApps()
            reply(it, "$apps")
        }
    }.join()
}

suspend fun getApps() = withContext(Dispatchers.IO)
{
    try {
        client.get<List<App>>("https://grey-source.herokuapp.com/apps")
    } catch (e: Exception) {
        println(e)
        emptyList()
    }
}


object Networking {
    val client = HttpClient(CIO) {
        // install(Logging)
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }


}