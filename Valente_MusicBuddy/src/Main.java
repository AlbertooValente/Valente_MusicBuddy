import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        try {
            String botToken = "7704256769:AAGZRn52Vf8rJKD0RX0Q-DS00h5J9744ogE";
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new MusicBuddy());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
