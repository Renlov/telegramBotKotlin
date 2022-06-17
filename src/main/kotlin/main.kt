import ConstValue.Companion.APPSFLYER
import ConstValue.Companion.APP_NAME
import Networking.client
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.utils.types.buttons.*
import dev.inmo.tgbotapi.extensions.utils.types.buttons.row
import dev.inmo.tgbotapi.extensions.utils.withContent
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import ConstValue.Companion.BUNDLE
import ConstValue.Companion.FBAPPID
import ConstValue.Companion.FBCLIENTSECRET
import ConstValue.Companion.NEXT
import ConstValue.Companion.URL
import ConstValue.Companion.nameReplyMarkup
import com.benasher44.uuid.uuid4
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviour
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.setWebhookInfoAndStartListenWebhooks
import dev.inmo.tgbotapi.requests.webhook.SetWebhook
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.tomcat.*
import kotlinx.coroutines.*
import kotlin.collections.ArrayList

suspend fun main() {
    val scope = CoroutineScope(Dispatchers.IO)
    val subRoute = uuid4().toString()

    telegramBotWithBehaviour(System.getenv("KEYTELEGRAM"), scope = scope) {
        setWebhookInfoAndStartListenWebhooks(
            System.getenv("PORT").toInt(),
            Tomcat,
            SetWebhook("https://telegrambotgrey.herokuapp.com/$subRoute"),
            {
                it.printStackTrace()
            },
            "0.0.0.0",
            subRoute,
            scope = this,
            block = asUpdateReceiver
        )
        println(getMe())

        onUnhandledCommand {
            println(it.chat.id)
            onCommand("info") { textComponent ->
                sendMessage(
                    textComponent.chat, "Hello, this is a gray1 department bot.\n" +
                            "In this chat you can add apps, search + correct data and find all apps\n\n" +
                            "You have this commands:\n" +
                            "/apps - find all apps\n" +
                            "/search - to find app information and correct data\n" +
                            "/put - to add app in database\n\n" +
                            "if bot asleep, open link in browser to wake up bot\n" +
                            "https://telegrambotgrey.herokuapp.com\n" +
                            "АНЯ, если что-то сломала, не трогай больше ничего и напиши нам!"
                )
            }
            onCommand("apps") {component ->
                sendMessage(component.chat, "wait...(sometimes more 10 second)")
                val apps = getApps()
                println("${component.chat} ${component.chat.id.chatId}")
                apps.forEach { app ->
                    bot.sendMessage(component.chat, appToString(app))
                }
            }
            onCommand("search") { onCommandChat ->
                val searchBundle = waitText(
                    SendTextMessage(
                        onCommandChat.chat.id,
                        "input $BUNDLE"
                    )
                )
                val findApp: App = getCurrentApp(searchBundle.first().text) as App
                println(findApp.toString())
                bot.sendTextMessage(onCommandChat.chat, appToString(findApp), replyMarkup = inlineKeyboard {
                    row {
                        includePageButtons()
                    }
                })
                var name = ""
                this.onMessageDataCallbackQuery {massageData ->
                    name += massageData.data
                    println(name)
                    bot.editMessageText(
                        massageData.message.withContent() ?: massageData.let { app ->
                            answer(app, "Unsupported message type :(")
                            return@onMessageDataCallbackQuery
                        },
                        "change $name on ${findApp.appName} app",
                        replyMarkup = inlineKeyboard {
                        }
                    )
                }
                val changeData = waitText().first().text

                changeDataApp(name, changeData, findApp)
                replaceCurrentApp(findApp)
                name.drop(name.length)

                bot.sendMessage(onCommandChat.chat, "Done ${appToString(findApp)}")
            }
            onCommand("add") { massage ->
                val arrayList = ArrayList<String?>()
                val bundle = waitText(
                    SendTextMessage(
                        massage.chat.id, "Bundle, like\ncom.opple.entel\nАНЯ, ЕГО НЕЛЬЗЯ БУДЕТ ПОМЕНЯТЬ!"
                    )
                )
                arrayList.add(bundle[0].text.lowercase().replace(" ", ""))
                val name = waitText(
                    SendTextMessage(
                        massage.chat.id, "App name, like\nSoul of Apis"
                    )
                )
                arrayList.add(name[0].text)
                val link = waitText(
                    SendTextMessage(
                        massage.chat.id, "Url, like\nhttps://www.dssm.us/21006Db01", replyMarkup = nameReplyMarkup
                    )
                ).first().text.takeIf { it != NEXT }
                if (link != null) {
                    arrayList.add(link)
                } else arrayList.add(null)
                val apps = waitText(
                    SendTextMessage(
                        massage.chat.id, "AppsFlyer, like\nmciwvaFyjHeFMHFokEfuLE", replyMarkup = nameReplyMarkup
                    )
                ).first().text.takeIf { it != NEXT }
                if (apps != null) {
                    arrayList.add(apps)
                } else arrayList.add(null)
                val deepLink = waitText(
                    SendTextMessage(
                        massage.chat.id, "Facebook appId, like\n1349989478796692", replyMarkup = nameReplyMarkup
                    )
                ).first().text.takeIf { it != NEXT }
                if (deepLink != null) {
                    arrayList.add(deepLink)
                } else arrayList.add(null)

                val appMarker = waitText(
                    SendTextMessage(
                        massage.chat.id, "Facebook appMarker, like\n599f0f527ce2d13678e170d47e2a6cf3", replyMarkup = nameReplyMarkup
                    )
                ).first().text.takeIf { it != NEXT }
                if (appMarker != null) {
                    arrayList.add(appMarker)
                } else arrayList.add(null)

                println(massage.chat.id)


                val app = App(arrayList[0]!!, arrayList[1]!!, arrayList[2], arrayList[3], arrayList[4], arrayList[5])
                postCurrentApp(app)
                sendMessage(massage.chat, "Done\n${appToString(app)}", replyMarkup = ReplyKeyboardRemove(false))
            }
        }.join()
    }

    scope.coroutineContext.job.join()
}

suspend fun postCurrentApp(app : App) = withContext(Dispatchers.IO){
    try {
        client.post("https://grey-source.herokuapp.com/add_app"){
            contentType(ContentType.Application.Json)
            setBody(app)
        }.body<App>()
    }catch (e : Exception){
        println(e)
    }
}

suspend fun replaceCurrentApp(app: App) = withContext(Dispatchers.IO){
    try {
        client.post("https://grey-source.herokuapp.com/replace_app"){
            contentType(ContentType.Application.Json)
            setBody(app)
        }.body<App>()
    }catch (e : Exception){
        println(e)
    }
}

suspend fun getCurrentApp(bundle : String) = withContext(Dispatchers.IO){
    try {
        client.get("https://grey-source.herokuapp.com/apps?search=$bundle").body<App>()
    } catch (e : Exception) {
        println(e)
    }
}

suspend fun getApps() = withContext(Dispatchers.IO)
{
    try {
        client.get("https://grey-source.herokuapp.com/apps").body<List<App>>()
    } catch (e: Exception) {
        println(e)
        emptyList()
    }
}

object Networking {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
}
fun InlineKeyboardBuilder.includePageButtons() {
    val firstLineButton = listOfNotNull(
        APP_NAME,
        URL,
        APPSFLYER,
    )

    val secondLineButton =listOfNotNull(
        FBAPPID,
        FBCLIENTSECRET
    )

    row {
        firstLineButton.forEach {
            dataButton(it, it)
        }

    }
    row {
        secondLineButton.forEach {
            dataButton(it, it)
        }
    }
}

fun changeDataApp(source : String, param : String, app: App) : Unit = when(source){
    APP_NAME ->{
        app.appName = param
    }
    URL ->{
        app.source = param
    }
    APPSFLYER ->{
        app.appsFlyer = param
    }
    FBAPPID ->{
        app.fbAppId = param
    }
    FBCLIENTSECRET ->{
        app.fbClientSecret = param
    }
    else ->{
    }
}

fun appToString(app: App): String {
    return "$BUNDLE = ${app.bundle}\n$APP_NAME = ${app.appName}\n$URL = ${app.source}\n" +
            "$APPSFLYER = ${app.appsFlyer}\n$FBAPPID = ${app.fbAppId}\n" +
            "$FBCLIENTSECRET = ${app.fbClientSecret}"
}

class ConstValue{
    companion object {
        const val BUNDLE = "bundle"
        const val APP_NAME = "app name"
        const val URL = "url"
        const val APPSFLYER = "appsFlyer"
        const val FBAPPID = "Facebook appId"
        const val FBCLIENTSECRET = "Facebook appMarker"
        const val NEXT = "Пропустить"
        val nameReplyMarkup = ReplyKeyboardMarkup(
            matrix {
                row {
                    +SimpleKeyboardButton(NEXT)
                }
            }
        )
    }
}
