import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
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
                String message = "Ciao! Benvenuto su MusicBuddy!\n" +
                        "Sono qui per aiutarti a scoprire nuove canzoni, artisti e album!\n" +
                        "Puoi usare il comando /cerca per esplorare il nostro database.";

                inviaMessaggio(chatId, message);

            } else if (receivedText.startsWith("/cerca")) {
                comandoCerca(chatId, receivedText);

            } else if (receivedText.startsWith("/testo")) {
                userStates.put(chatId, new String[]{null, null, null});
                inviaMessaggio(chatId, "Scrivi il titolo della canzone che vuoi cercare");

            } else if (receivedText.startsWith("/curiosita")) {
                comandoCuriosita(chatId);

            } else if (receivedText.startsWith("/suggerisci")) {

            } else if (receivedText.startsWith("/help")) {
                String message = "LISTA COMANDI:\n" +
                        "/cerca - Cerca informazioni su una canzone, un artista o un album\n" +
                        "/testo - Restituisce il testo di una canzone specifica\n" +
                        "/curiosita - Restituisce una curiosità musicale giornaliera\n" +
                        "/suggerisci - Suggerisce canzoni o playlist in base ai tuoi interessi musicali";

                inviaMessaggio(chatId, message);

            } else {
                inviaMessaggio(chatId, "Comando non riconosciuto! Usa /help per sapere quali comandi utilizzare");
            }
        }
    }

    //metodo che gestisce il comando cerca
    private void comandoCerca(String chatId, String receivedText) {
        String[] parts = receivedText.split(" ", 2);

        if (parts.length < 2) {
            inviaMessaggio(chatId, "Specifica il tipo di ricerca:\n/cerca artista\n/cerca album\n/cerca canzone");
            return;
        }

        String tipo = parts[1].toLowerCase();

        if (!tipo.equals("artista") && !tipo.equals("album") && !tipo.equals("canzone")) {
            inviaMessaggio(chatId, "Tipo non riconosciuto! Usa: artista, album o canzone");
            return;
        }

        //inizializza lo stato dell'utente
        userStates.put(chatId, new String[]{tipo, null, null}); //tipo, nome elemento, artista

        if (tipo.equals("artista")) {
            inviaMessaggio(chatId, "Scrivi il nome dell'" + tipo + " che vuoi cercare");
        } else if (tipo.equals("canzone")) {
            inviaMessaggio(chatId, "Scrivi il titolo della " + tipo + " che vuoi cercare");
        } else {
            inviaMessaggio(chatId, "Scrivi il titolo dell'" + tipo + " che vuoi cercare");
        }
    }

    private void gestioneUtenteRicerca(String chatId, String receivedText) {
        String[] state = userStates.get(chatId);
        String tipo = state[0];
        String nomeElemento = state[1];
        String artista = state[2];

        if (nomeElemento == null) {     //salva il nome dell'artista/il titolo dell'album/canzone
            state[1] = addMaiuscole(receivedText);

            if (tipo != null && tipo.equals("artista")) {
                userStates.remove(chatId);
                cercaArtista(chatId, state[1]);
            } else {
                inviaMessaggio(chatId, "Ora scrivi il nome dell'artista");
            }
        } else if (artista == null) {   //salva il nome dell'artista se si sta cercando una canzone o un album
            state[2] = addMaiuscole(receivedText);
            userStates.remove(chatId); //rimuove lo stato dell'utente

            try{
                String result = dbManager.getArtistaByNome(state[2]);

                //se l'artista non è nel database, fa prima lo scraping
                if (result.equals("")) {
                    inviaMessaggio(chatId, "Ricerca in corso...");
                    WebScraper.cercaArtista(state[2]);
                }

                //esegue la ricerca
                if (tipo == null) {
                    cercaTesto(chatId, nomeElemento, state[2]);
                } else if (tipo.equals("album")) {
                    cercaAlbum(chatId, nomeElemento, state[2]);
                } else if (tipo.equals("canzone")) {
                    cercaCanzone(chatId, nomeElemento, state[2]);
                }

            } catch(SQLException e){
                e.printStackTrace();
            } catch (IOException e) {
                inviaMessaggio(chatId, "Errore nella ricerca! Controllare di aver scritto correttamente il nome dell'artista!");
                e.printStackTrace();
            }
        }
    }

    //metodo che mette le maiuscole ad ogni parola separata da uno spazio
    private String addMaiuscole(String input) {
        //lista di alcune eccezioni
        List<String> eccezioni = Arrays.asList("AC/DC", "DJ", "OF", "A");

        return Arrays.stream(input.split(" "))
                .map(word -> {
                    //se la parola è nelle eccezioni, viene lasciata invariata
                    if (eccezioni.contains(word.toUpperCase())) {
                        return word;
                    }

                    return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
                })
                .collect(Collectors.joining(" "));
    }

    //metodo per la ricerca dell'artista
    private void cercaArtista(String chatId, String artista) {
        try {
            //cerca nel database se c'è già l'artista
            String result = dbManager.getArtistaByNome(artista);

            if (!result.equals("")) {
                inviaMessaggio(chatId, result);
                return;
            }

            //se non lo trova, prova a fare scraping
            inviaMessaggio(chatId, "Ricerca in corso...");
            WebScraper.cercaArtista(artista);

            //controlla di nuovo il database dopo lo scraping
            result = dbManager.getArtistaByNome(artista);

            if (!result.equals("")) {
                inviaMessaggio(chatId, result);
            } else {
                inviaMessaggio(chatId, "Artista non trovato!");
            }
        } catch (IOException e) {
            inviaMessaggio(chatId, "Errore durante la ricerca sul web dell'artista");
            e.printStackTrace();

        } catch (SQLException e) {
            inviaMessaggio(chatId, "Errore nella ricerca dell'artista nel database");
            e.printStackTrace();
        }
    }

    //metodo per la ricerca dell'album
    private void cercaAlbum(String chatId, String album, String artista) {
        try {
            String result = dbManager.getAlbumByNome(album, artista);

            if (!result.equals("")) {
                inviaMessaggio(chatId, result);
            } else {
                inviaMessaggio(chatId, "Album non trovato!");
            }
        } catch (SQLException e) {
            inviaMessaggio(chatId, "Errore nella ricerca dell'album!");
            e.printStackTrace();
        }
    }

    //metodo per la ricerca della canzone
    private void cercaCanzone(String chatId, String canzone, String artista) {
        try {
            List<String> result = dbManager.getCanzoneByNome(canzone, artista);

            if (!result.isEmpty()) {
                inviaMessaggioMarkup(chatId, result.get(0), result.get(1), result.get(2));
            } else {
                inviaMessaggio(chatId, "Canzone non trovata!");
            }
        } catch (SQLException e) {
            inviaMessaggio(chatId, "Errore nella ricerca della canzone!");
            e.printStackTrace();
        }
    }

    //metodo per la ricerca del testo di una canzone
    private void cercaTesto(String chatId, String canzone, String artista) {
        try {
            String result = dbManager.getTestoCanzone(canzone, artista);

            if (!result.equals("")) {
                inviaMessaggio(chatId, result);
                return;
            }

            //se non trova la canzone, prova a fare scraping
            inviaMessaggio(chatId, "Sto cercando informazioni sul testo di " + canzone + "...");
            WebScraper.cercaArtista(artista);

            //controlla di nuovo il database dopo lo scraping
            result = dbManager.getTestoCanzone(canzone, artista);

            if (result.equals("")) {
                inviaMessaggio(chatId, result);
            } else {
                inviaMessaggio(chatId, "Testo della canzone non trovato!");
            }

        } catch (IOException e) {
            inviaMessaggio(chatId, "Errore durante la ricerca sul web del testo della canzone!");
            e.printStackTrace();
        } catch (SQLException e) {
            inviaMessaggio(chatId, "Errore nella ricerca del testo della canzone!");
            e.printStackTrace();
        }
    }

    //metodo che gestisce il comando curiosita
    private void comandoCuriosita(String chatId){
        //ottiene la data di oggi
        LocalDate oggi = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dataFormattata = oggi.format(formatter);

        //formattazione della data per il messaggio
        String message = "";
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
        String dataFormattataMessage = dateFormat.format(java.sql.Date.valueOf(oggi));

        // Inizializza le informazioni sugli album e le canzoni
        List<String> albumMessaggi = new ArrayList<>();
        List<String> canzoniMessaggi = new ArrayList<>();

        try {
            //cerca album usciti oggi
            List<String> albumOggi = dbManager.getAlbumByDataUscita(dataFormattata);

            //se ci sono album, aggiungi le info dell'album al messaggio
            if (!albumOggi.isEmpty()) {
                for (String album : albumOggi) {
                    albumMessaggi.add(String.format("*Oggi (%s) è uscito l'album:*\n%s", dataFormattataMessage, album));
                }
            }

            //cerca canzoni uscite oggi
            List<String> canzoniOggi = dbManager.getCanzoneByDataUscita(dataFormattata);

            //aggiunge le canzoni che non appartengono già agli album inseriti nel messaggio
            for (String canzone : canzoniOggi) {
                if (!albumMessaggi.contains(canzone)) {
                    canzoniMessaggi.add(String.format("*Oggi (%s) è uscita la canzone:*\n%s", dataFormattataMessage, canzone));
                }
            }

            //se non trova nulla, invia un messaggio alternativo
            if (albumMessaggi.isEmpty() && canzoniMessaggi.isEmpty()) {
                message = "Oggi non ci sono curiosità, riprova domani!";
            } else {
                //aggiunge gli album al messaggio
                for (String album : albumMessaggi) {
                    message += album + "\n\n";
                }

                //aggiunge le canzoni al messaggio
                for (String canzone : canzoniMessaggi) {
                    message += canzone + "\n\n";
                }
            }

            // Invia il messaggio
            inviaMessaggio(chatId, message);
        } catch (SQLException e) {
            e.printStackTrace();
            inviaMessaggio(chatId, "Si è verificato un errore nel recuperare le curiosità!");
        }
    }

    //metodo che gestisce l'invio dei messaggi di risposta
    private void inviaMessaggio(String chatId, String message) {
        String mess = escapeMarkdownV2(message);
        SendMessage sendMessage = new SendMessage(chatId, mess);
        sendMessage.setParseMode("MarkdownV2");     //imposta il tipo di parsing MarkdownV2

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //metodo che gestisce l'invio del messaggio di risposta con i link di spotify e youtube
    private void inviaMessaggioMarkup(String chatId, String message, String linkSpotify, String linkYoutube) {
        //crea la lista di bottoni sotto il messaggio
        List<InlineKeyboardRow> rowsInline = new ArrayList<>();

        //spotify
        if (!linkSpotify.equals("Non disponibile")) {
            InlineKeyboardButton spotifyButton = new InlineKeyboardButton("Ascolta su Spotify 🎧");
            spotifyButton.setUrl(linkSpotify);

            InlineKeyboardRow spotifyRow = new InlineKeyboardRow();
            spotifyRow.add(spotifyButton);
            rowsInline.add(spotifyRow);
        }

        //youtube
        if (!linkYoutube.equals("Non disponibile")) {
            InlineKeyboardButton youtubeButton = new InlineKeyboardButton("Ascolta su YouTube 📺");
            youtubeButton.setUrl(linkYoutube);

            InlineKeyboardRow youtubeRow = new InlineKeyboardRow();
            youtubeRow.add(youtubeButton);
            rowsInline.add(youtubeRow);
        }

        //crea il markup con la lista di righe
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rowsInline);

        //invia il messaggio con il markup
        String mess = escapeMarkdownV2(message);
        SendMessage sendMessage = new SendMessage(chatId, mess);
        sendMessage.setParseMode("MarkdownV2");
        sendMessage.setReplyMarkup(markup);

        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //per evitare il parsing di tutti i principali caratteri speciali
    private String escapeMarkdownV2(String text) {
        return text.replaceAll("([\\\\\\[_\\]`()~>#|+=\\-{}.!])", "\\\\$1");
    }
}