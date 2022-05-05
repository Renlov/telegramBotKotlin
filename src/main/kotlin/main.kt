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
import dev.inmo.tgbotapi.types.buttons.KeyboardButton
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
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
import java.awt.Button
import java.util.*
import kotlin.collections.ArrayList

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

        onCommand("put"){
            val arrayList = ArrayList<String>()

            val nameReplyMarkup = ReplyKeyboardMarkup(
                matrix {
                    row {
                        +SimpleKeyboardButton("Пропустить")
                    }
                }
            )

            val bundle = waitText(
                SendTextMessage(
                    it.chat.id, "Bundle, like\ncom.opple.entel")
            )
            arrayList.add(bundle[0].text)

            val name = waitText(
                SendTextMessage(
                    it.chat.id, "App name, like\nSoul of Apis")
            )
            arrayList.add(name[0].text)

            val link = waitText(
                SendTextMessage(
                    it.chat.id, "Url, like\nhttps://www.dssm.us/21006Db01", replyMarkup = nameReplyMarkup)
            ).first().text.takeIf { it != "Пропустить"}
            if (link !=null){
                arrayList.add(link)
            }else arrayList.add("")

            val apps = waitText(
                SendTextMessage(
                    it.chat.id, "AppsFlyer, like\nmciwvaFyjHeFMHFokEfuLE", replyMarkup = nameReplyMarkup)
            ).first().text.takeIf { it != "Пропустить"}
            if (apps != null){
                arrayList.add(apps)
            }else arrayList.add("")

            val fb = waitText(
                SendTextMessage(
                    it.chat.id, "FaceBook, like\n1349989478796692", replyMarkup = nameReplyMarkup)
            ).first().text.takeIf { it != "Пропустить"}
            if (fb !=null){
                arrayList.add(fb)
            }else arrayList.add("")
            val app = App(arrayList[0],arrayList[1],arrayList[2],arrayList[3],arrayList[4])
            postCurrentApp(app)

            bot.sendMessage(it.chat, "Done\n$app", replyMarkup = ReplyKeyboardRemove(false))
        }
    }.join()
}
suspend fun postCurrentApp(app : App) = withContext(Dispatchers.IO){
    try {
       client.post<App>("https://grey-source.herokuapp.com/add_app"){
           contentType(ContentType.Application.Json)
           body = app
       }
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
