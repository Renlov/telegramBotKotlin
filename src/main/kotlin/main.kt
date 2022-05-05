import Networking.client
import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun main() {
    val bot = telegramBot("5368599889:AAHMGBROXto1fsOLpM4kikjnaALLjA_c8ww")

    bot.buildBehaviourWithLongPolling {
        println(getMe())

        onCommand("apps") {
            bot.sendMessage(it.chat, "Ищу...")
            val apps = getApps()
            apps.forEach{app->
                bot.sendMessage(it.chat, app.toString())
            }
        }

        onCommand("search"){
            val text = waitText(
                SendTextMessage(
                    it.chat.id,
                "input bundle"
                )
            )
            val textS : String = getCurrentApp(text.first().text).toString()
            bot.sendMessage(it.chat, textS)

        }

        onCommand("post"){
            val text = waitText(
                SendTextMessage(
                    it.chat.id,
                    "fill this form\n\n\"bundle\":\"\",\n" +
                            "\"appName\":\"\",\n" +
                            "\"source\":\"\",\n" +
                            "\"appsFlyer\":\"\",\n" +
                            "\"fb\":\"\""
                )
            )
            val app = App(text[0].text, text[1].text, text[3].text, text[4].text, text[5].text)
            println(text[0].text)
            println(text.first().text)
            postCurrentApp(app)
        }
    }.join()
}

suspend fun postCurrentApp(app : App) = withContext(Dispatchers.IO){
    try {
        val formData = Parameters.build {
            append("bundle", app.bundle)
            append("appName", app.appName)
            append("source", app.source?:"")
            append("appsFlyer", app.appsFlyer?:"")
            append("fb", app.fb?:"")
        }

        val data : String = client.submitForm("https://grey-source.herokuapp.com/add_app", formData.toString(), encodeInQuery = true)
        print(data)
        //client.post<App>("https://grey-source.herokuapp.com/add_app")
    }catch (e : Exception){
        println(e)
    }
}

suspend fun getCurrentApp(bundle : String) = withContext(Dispatchers.IO){
    try {
        client.get<App>("https://grey-source.herokuapp.com/apps?search=$bundle")
    } catch (e : Exception) {
        println(e)
    }
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
        install(Logging)
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }
}
