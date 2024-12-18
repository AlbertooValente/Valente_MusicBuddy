import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class APIGenius {
    private static final String accessToken = Config.getAPIGeniusTOken();   //mi prendo il token dal file
    private static final HttpClient client = HttpClient.newHttpClient();

    //metodo per effettuare una ricerca su Genius per ottenere l'apipath della canzone
    public static JsonObject cercaCanzoneGenius(String urlRichiesta) {
        try {
            String url = "https://api.genius.com/search?q=" + urlRichiesta;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return JsonParser.parseString(response.body()).getAsJsonObject();
            } else {
                System.out.println("Errore durante la ricerca su Genius. Codice di stato: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Errore durante la ricerca su Genius: " + e.getMessage());
        }

        return null;
    }

    //metodo per ottenere i dettagli della canzone
    public static String[] getDettagli(String apiPath) {
        try {
            String url = "https://api.genius.com" + apiPath;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject dettagli = JsonParser.parseString(response.body()).getAsJsonObject()
                        .getAsJsonObject("response")
                        .getAsJsonObject("song");

                //estrai l'elemento media contenente i link di spotify e youtube
                JsonArray media = dettagli.getAsJsonArray("media");

                String spotifyUrl = null;
                String youtubeUrl = null;

                for (int i = 0; i < media.size(); i++) {
                    JsonObject mediaObject = media.get(i).getAsJsonObject();
                    String provider = mediaObject.get("provider").getAsString();

                    if (provider.equals("spotify")) {
                        spotifyUrl = mediaObject.get("url").getAsString();
                    } else if (provider.equals("youtube")) {
                        youtubeUrl = mediaObject.get("url").getAsString();
                    }
                }

                //estrae il path per i testi
                String lyricsPath = dettagli.get("path").getAsString();

                //estrae la data di pubblicazione della canzone
                String data = dettagli.get("release_date").getAsString();

                //estrae la descrizione
                JsonObject descriptionDom = dettagli.getAsJsonObject("description")
                        .getAsJsonObject("dom");    //il nodo dom contiene tutta la descrizione

                String descrizione = estraiTestoDescrizione(descriptionDom);

                // Restituisce i dati estratti
                return new String[]{spotifyUrl, youtubeUrl, lyricsPath, data,descrizione};
            } else {
                System.err.println("Errore durante il recupero dei dettagli della canzone. Codice di stato: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Errore durante il recupero dei dettagli della canzone: " + e.getMessage());
        }

        // Restituisci un array con valori null in caso di errore
        return new String[]{null, null};
    }

    //metodo per estrarre solo il testo della descrizione (togliendo tutti i riferimenti)
    private static String estraiTestoDescrizione(JsonObject domNode) {
        StringBuilder text = new StringBuilder();

        //controlla il tipo di nodo
        if (domNode.has("tag")) {
            if (domNode.has("children")) {
                JsonArray children = domNode.getAsJsonArray("children");

                for (JsonElement child : children) {
                    if (child.isJsonPrimitive()) {
                        text.append(child.getAsString());   //aggiunge il testo
                    } else if (child.isJsonObject()) {
                        text.append(estraiTestoDescrizione(child.getAsJsonObject()));   //ricorsione per nodi figli
                    }
                }
            }

            //aggiunge una nuova riga se il tag è un blocco (ad esempio "p")
            String tag = domNode.get("tag").getAsString();

            if ("p".equals(tag)) {
                text.append("\n\n");    //separa i vari paragrafi con una riga vuota
            }
        }

        return text.toString().trim();
    }
}
