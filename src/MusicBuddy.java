import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MusicBuddy implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient = new OkHttpTelegramClient(Config.getBotToken());
    private final DB_Manager dbManager = new DB_Manager();
    private final Map<String, String[]> userStates = new HashMap<>();   //HashMap per tracciare lo stato degli utenti, visto che il comando cerca prevede più messaggi

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            //recupera il messaggio inviato dall'utente
            String receivedText = update.getMessage().getText();
            String chatId = String.valueOf(update.getMessage().getChatId());

            //controlla lo stato dell'utente, ovvero controlla se l'utente sta eseguendo una ricerca di artista/canzone/album
            if (userStates.containsKey(chatId)) {
                gestioneUtenteRicerca(chatId, receivedText);
                return;
            }

            //verifica se il messaggio è il comando /start
            if (receivedText.equals("/start")) {
                comandoStart(chatId);
            }
            else if (receivedText.startsWith("/cerca")) {
                comandoCerca(chatId, receivedText);
            }
            else {
                sendMessage(chatId, "Comando non riconosciuto. Usa /start o /cerca per iniziare.");
            }
        }
    }

    //metodo che gestisce il comando start: invia un messaggio di benvenuto
    private void comandoStart(String chatId) {
        String message = "Ciao! Benvenuto su MusicBuddy!\n" +
                "Sono qui per aiutarti a scoprire nuove canzoni, artisti e album!\n" +
                "Puoi usare il comando /cerca per esplorare il nostro database.";

        sendMessage(chatId, message);
    }

    //metodo che gestisce il comando cerca
    private void comandoCerca(String chatId, String receivedText){
        String[] parts = receivedText.split(" ", 2);

        if (parts.length < 2) {
            sendMessage(chatId, "Specifica il tipo di ricerca:\n/cerca artista\n/cerca album\n/cerca canzone");
            return;
        }

        String tipo = parts[1].toLowerCase();

        if (!tipo.equals("artista") && !tipo.equals("album") && !tipo.equals("canzone")) {
            sendMessage(chatId, "Tipo non riconosciuto! Usa: artista, album o canzone");
            return;
        }

        //inizializza lo stato dell'utente
        userStates.put(chatId, new String[]{tipo, null, null}); //tipo, nome elemento, artista

        if(tipo.equals("artista")){
            sendMessage(chatId, "Scrivi il nome dell'" + tipo + " che vuoi cercare");
        }
        else if(tipo.equals("canzone")){
            sendMessage(chatId, "Scrivi il titolo della " + tipo + " che vuoi cercare");
        }
        else{
            sendMessage(chatId, "Scrivi il titolo dell'" + tipo + " che vuoi cercare");
        }
    }

    private void gestioneUtenteRicerca(String chatId, String receivedText) {
        String[] state = userStates.get(chatId);
        String tipo = state[0];
        String nomeElemento = state[1];
        String artista = state[2];

        if (nomeElemento == null) {     //salva il nome dell'elemento (artista, canzone o album)
            state[1] = addMaiuscole(receivedText);

            if(tipo.equals("artista")){
                userStates.remove(chatId);
                cercaArtista(chatId, state[1]);
            }
            else{
                sendMessage(chatId, "Ora scrivi il nome dell'artista");
            }

        } else if (artista == null) {   //salva il nome dell'artista se si sta cercando una canzone o un album
            state[2] = addMaiuscole(receivedText);
            userStates.remove(chatId); //rimuove lo stato dell'utente

            //esegue la ricerca
            if (tipo.equals("album")) {
                //cercaAlbum(chatId, nomeElemento, state[2]);
            } else if (tipo.equals("canzone")) {
                //cercaCanzone(chatId, nomeElemento, state[2]);
            }
        }
    }

    //metodo che mette le maiuscole ad ogni parola separata da uno spazio
    private String addMaiuscole(String input) {
        return Arrays.stream(input.split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    //metodo per la ricerca dell'artista
    private void cercaArtista(String chatId, String artista) {
        try {
            //cerca nel database se c'è già l'artista
            String result = dbManager.getArtistaByNome(artista);

            if (result != null) {
                sendMessage(chatId, result);
                return;
            }

            //se non lo trova, prova a fare scraping
            sendMessage(chatId, "Sto cercando ulteriori informazioni su " + artista + "...");
            WebScraper.cercaArtista(artista, "artista");

            //controlla di nuovo il database dopo lo scraping
            result = dbManager.getArtistaByNome(artista);

            if (result != null) {
                sendMessage(chatId, result);
            } else {
                sendMessage(chatId, "Artista non trovato!");
            }
        } catch (IOException e) {
            sendMessage(chatId, "Errore durante la ricerca sul web per l'artista");
            e.printStackTrace();

        } catch (SQLException e) {
            sendMessage(chatId, "Errore nella ricerca dell'artista nel database");
            e.printStackTrace();
        }
    }

    /*
    private void cercaAlbum(String chatId, String album, String artista) {
        try {
            int idArtista = dbManager.getIdArtista(artista);
            if (idArtista == 0) {
                sendMessage(chatId, "Artista non trovato per l'album.");
                return;
            }

            String result = dbManager.getAlbumByNome(album);
            if (result != null) {
                sendMessage(chatId, "Album trovato:\n" + result);
            } else {
                sendMessage(chatId, "Album non trovato.");
            }
        } catch (SQLException e) {
            sendMessage(chatId, "Errore nella ricerca dell'album.");
            e.printStackTrace();
        }
    }
    */

    /*
    private void cercaCanzone(String chatId, String canzone, String artista) {
        try {
            int idArtista = dbManager.getIdArtista(artista);
            if (idArtista == 0) {
                sendMessage(chatId, "Artista non trovato per la canzone.");
                return;
            }

            String result = dbManager.getCanzoneByNome(canzone);
            if (result != null) {
                sendMessage(chatId, "Canzone trovata:\n" + result);
            } else {
                sendMessage(chatId, "Canzone non trovata.");
            }
        } catch (SQLException e) {
            sendMessage(chatId, "Errore nella ricerca della canzone.");
            e.printStackTrace();
        }
    }
     */

    //metodo che gestisce l'invio dei messaggi di risposta
    private void sendMessage(String chatId, String message) {
        String mess = escapeMarkdownV2(message);
        SendMessage sendMessage = new SendMessage(chatId, mess);
        sendMessage.setParseMode("MarkdownV2");     //imposta il tipo di parsing MarkdownV2

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //per evitare il parsing di tutti i principali caratteri speciali
    private String escapeMarkdownV2(String text) {
        return text.replaceAll("([\\\\+\\-_\\[\\]()`>#+!|{}.])", "\\\\$1");
    }
}