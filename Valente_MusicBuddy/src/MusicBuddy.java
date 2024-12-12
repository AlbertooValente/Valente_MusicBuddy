import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MusicBuddy implements LongPollingSingleThreadUpdateConsumer {
    private TelegramClient telegramClient = new OkHttpTelegramClient("7704256769:AAGZRn52Vf8rJKD0RX0Q-DS00h5J9744ogE");

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            //recupera il messaggio inviato dall'utente
            String receivedText = update.getMessage().getText();

            //verifica se il messaggio è il comando /start
            if (receivedText.equals("/start")) {
                //crea un messaggio di benvenuto
                String chatId = String.valueOf(update.getMessage().getChatId());
                String message = "Ciao! Benvenuto su MusicBuddy!\n" +
                        "Sono qui per aiutarti a scoprire nuove canzoni, artisti e testi!" +
                        "\nCosa posso fare per te oggi?";
                SendMessage sendMessage = new SendMessage(chatId, message);

                try {
                    // Esegui il messaggio di benvenuto
                    telegramClient.execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}