import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        try {
            String botToken = Config.getBotToken();
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new MusicBuddy());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
