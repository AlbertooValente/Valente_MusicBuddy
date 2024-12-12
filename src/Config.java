import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    public static String getBotToken() {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        }
        catch (IOException e){
            System.out.println("Errore nella lettura del file 'config.properties'");
        }

        return properties.getProperty("BOT_TOKEN");
    }
}
