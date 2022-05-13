import ConstValue.Companion.ALL
import ConstValue.Companion.APPSFLYER
import ConstValue.Companion.APP_NAME
import Networking.client
import dev.inmo.tgbotapi.bot.Ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.answers.answer
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
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
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import ConstValue.Companion.BUNDLE
import ConstValue.Companion.DEEPLINK
import ConstValue.Companion.NEXT
import ConstValue.Companion.URL
import ConstValue.Companion.nameReplyMarkup
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviour
import dev.inmo.tgbotapi.extensions.utils.updates.flowsUpdatesFilter
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.setWebhookInfoAndStartListenWebhooks
import dev.inmo.tgbotapi.requests.webhook.SetWebhook
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.updateshandlers.FlowsUpdatesFilter
import kotlinx.coroutines.*
import org.apache.catalina.startup.Tomcat
import java.util.*
import kotlin.collections.ArrayList

suspend fun main() {
        val bot = telegramBot(System.getenv("KEYTELEGRAM"))
        val scope = CoroutineScope(Dispatchers.Default)
    val subroute = UUID.randomUUID().toString()
    val filter = FlowsUpdatesFilter()


    val server = bot.setWebhookInfoAndStartListenWebhooks(System.getenv("PORT").toInt(), io.ktor.server.tomcat.Tomcat, SetWebhook(
        "https://telegrambotgrey.herokuapp.com/$subroute", allowedUpdates = filter.allowedUpdates), {

    }, "0.0.0.0", subroute, scope = scope, block = filter.asUpdateReceiver)

    server.environment.connectors.forEach{
        println(it)
    }
    server.start(false)



       bot.buildBehaviour(filter) {
            println(getMe())
            onCommand("apps") {
                bot.sendMessage(it.chat, "wait...")
                val apps = getApps()
                apps.forEach { app ->
                    bot.sendMessage(it.chat, appToString(app))
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
                bot.sendTextMessage(onCommandChat.chat, appToString(findApp), replyMarkup = inlineKeyboard {
                    row {
                        includePageButtons()
                    }
                })
                onMessageDataCallbackQuery {
                    val name = it.data
                    editMessageText(
                        it.message.withContent() ?: it.let {
                            answer(it, "Unsupported message type :(")
                            return@onMessageDataCallbackQuery
                        },
                        "change $name on ${findApp.appName} app",
                        replyMarkup = inlineKeyboard {
                        }
                    )
                    val changeData = waitText().first().text
                    changeDataApp(name, changeData, findApp)
                    replaceCurrentApp(findApp)
                    bot.sendMessage(onCommandChat.chat, "Done ${appToString(findApp)}")
                }
            }
            onCommand("put") {
                val arrayList = ArrayList<String?>()
                val bundle = waitText(
                    SendTextMessage(
                        it.chat.id, "Bundle, like\ncom.opple.entel"
                    )
                )
                arrayList.add(bundle[0].text)
                val name = waitText(
                    SendTextMessage(
                        it.chat.id, "App name, like\nSoul of Apis"
                    )
                )
                arrayList.add(name[0].text)
                val link = waitText(
                    SendTextMessage(
                        it.chat.id, "Url, like\nhttps://www.dssm.us/21006Db01", replyMarkup = nameReplyMarkup
                    )
                ).first().text.takeIf { it != NEXT }
                if (link != null) {
                    arrayList.add(link)
                } else arrayList.add(null)
                val apps = waitText(
                    SendTextMessage(
                        it.chat.id, "AppsFlyer, like\nmciwvaFyjHeFMHFokEfuLE", replyMarkup = nameReplyMarkup
                    )
                ).first().text.takeIf { it != NEXT }
                if (apps != null) {
                    arrayList.add(apps)
                } else arrayList.add(null)
                val deepLink = waitText(
                    SendTextMessage(
                        it.chat.id, "DeepLink, like\n1349989478796692", replyMarkup = nameReplyMarkup
                    )
                ).first().text.takeIf { it != NEXT }
                if (deepLink != null) {
                    arrayList.add(deepLink)
                } else arrayList.add(null)
                val app = App(arrayList[0]!!, arrayList[1]!!, arrayList[2], arrayList[3], arrayList[4])
                postCurrentApp(app)
                bot.sendMessage(it.chat, "Done\n${appToString(app)}", replyMarkup = ReplyKeyboardRemove(false))
            }.join()
        }
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

suspend fun replaceCurrentApp(app: App) = withContext(Dispatchers.IO){
    try {
        client.post<App>("https://grey-source.herokuapp.com/replace_app"){
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
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }
}

suspend fun BehaviourContext.addApp(it : CommonMessage<TextContent>){
    val arrayList = ArrayList<String?>()

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
    }else arrayList.add(null)

    val apps = waitText(
        SendTextMessage(
            it.chat.id, "AppsFlyer, like\nmciwvaFyjHeFMHFokEfuLE", replyMarkup = nameReplyMarkup)
    ).first().text.takeIf { it != "Пропустить"}
    if (apps != null){
        arrayList.add(apps)
    }else arrayList.add(null)

    val deepLink = waitText(
        SendTextMessage(
            it.chat.id, "DeepLink, like\n1349989478796692", replyMarkup = nameReplyMarkup)
    ).first().text.takeIf { it != "Пропустить"}
    if (deepLink !=null){
        arrayList.add(deepLink)
    }else arrayList.add(null)

    val app = App(arrayList[0]!!,arrayList[1]!!,arrayList[2],arrayList[3],arrayList[4])
    replaceCurrentApp(app)
    bot.sendMessage(it.chat, "Done ${appToString(app)}")
}

fun InlineKeyboardBuilder.includePageButtons() {
    val firstLineButton = listOfNotNull(
        BUNDLE,
        APP_NAME,
        URL,
    )

    val secondLineButton =listOfNotNull(
        APPSFLYER,
        DEEPLINK
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
    BUNDLE -> {
        app.bundle =  param
    }
    APP_NAME ->{
        app.appName = param
    }
    URL ->{
        app.source = param
    }
    APPSFLYER ->{
        app.appsFlyer = param
    }
    DEEPLINK ->{
        app.fb = param
    }
    ALL ->{

    }
        else ->{
    }
}

fun appToString(app: App): String {
    return "$BUNDLE = ${app.bundle}\n$APP_NAME = ${app.appName}\n$URL = ${app.source}\n" +
            "$APPSFLYER = ${app.appsFlyer}\n$DEEPLINK = ${app.fb}"
}

class ConstValue{
    companion object {
        const val BUNDLE = "bundle"
        const val APP_NAME = "app name"
        const val URL = "url"
        const val APPSFLYER = "appsFlyer"
        const val DEEPLINK = "deepLink"
        const val ALL = "all"
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

