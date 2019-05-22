package com.internship

import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

@RestController
class BotController {

    companion object {
        private val API_ENDPOINT = "https://api.telegram.org/bot"

        private val START_COMMAND = "/start"
    }

    private val logger: Logger = Logger.getLogger("[TimerBot]")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy в HH:mm МСК")
                                                 .withLocale(Locale("ru"))
                                                 .withZone(ZoneId.of("UTC+3"))

    @Value("\${token}")
    lateinit var token: String

    private fun formatDate(instant: Instant) = dateFormatter.format(instant)

    @PostMapping("/\${token}")
    fun onUpdate(@RequestBody update: Update) {
        logger.log(Level.INFO, "Got update: " + update)

        if (update.message != null) {
            val chatId = update.message.chat.id
            val text = update.message.text

            when {
                text?.startsWith(START_COMMAND) == true -> onStartCommand(chatId)
                else -> onNotifyRequest(Message(Chat(chatId), text))
            }
        }
    }

    private fun onStartCommand(chatId: Long) = try {
        sendMessage(chatId, "Привет! Я - TimerBot! Укажи время в формате \"1 час 15 минут\"," +
                " и я уведомлю тебя, когда это время пройдёт.")
    } catch (e: UnirestException) {
        logger.log(Level.SEVERE, "Cannot send message", e)
    }

    private fun onNotifyRequest(message: Message) {
        if (message.text == null) {
            sendMessage(message.chat.id, "Отправьте длительность, чтобы я мог оповестить Вас")
            return
        }

        val duration = parseRequest(message.text)
        if (duration == 0L) {
            sendMessage(message.chat.id, "Не понял Вас, попробуйте перефразировать запрос")
            return
        }

        val notifyDate = Instant.now().plusMillis(duration)
        GlobalScope.launch {
            delay(duration)
            notify(RemindMeRequest(message, notifyDate))
        }

        sendMessage(message.chat.id, "Хорошо! Уведомлю Вас ${formatDate(notifyDate)}")
    }

    @Throws(UnirestException::class)
    private fun sendMessage(chatId: Long, text: String) {
        Unirest.post("$API_ENDPOINT$token/sendMessage")
            .field("chat_id", chatId)
            .field("text", text)
            .asJson()
    }

    private fun notify(request: RemindMeRequest) {
        val notifyDate = request.instant
        sendMessage(request.msg.chat.id, "Время пришло! " +
                "(Вы просили уведомить Вас ${formatDate(notifyDate)})")
    }
}