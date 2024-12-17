import com.google.gson.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class APIGenius {
    private static final String accessToken = Config.getAPIGeniusTOken();

    public static void main(String[] args) {
        try {
            // Passaggio 1: Esegui la ricerca su Genius per "time pink floyd"
            JsonObject searchResult = searchSong("time%20pink%20floyd");

            // Passaggio 2: Prendi la prima hit dal risultato
            JsonObject firstHit = searchResult.getAsJsonObject("response")
                    .getAsJsonArray("hits")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("result");

            // Ottieni api_path dalla hit
            String apiPath = firstHit.get("api_path").getAsString();

            // Passaggio 3: Ottieni i dettagli della canzone usando l'api_path
            JsonObject songDetails = getSongDetails(apiPath);

            // Passaggio 4: Estrai le informazioni richieste (media, Spotify e YouTube)
            JsonArray media = songDetails.getAsJsonObject("response")
                    .getAsJsonObject("song")
                    .getAsJsonArray("media");

            // Cicla attraverso i media e cerca provider Spotify e YouTube
            for (int i = 0; i < media.size(); i++) {
                JsonObject mediaItem = media.get(i).getAsJsonObject();
                String provider = mediaItem.get("provider").getAsString();

                if (provider.equals("spotify")) {
                    System.out.println("Spotify URL: " + mediaItem.get("url").getAsString());
                } else if (provider.equals("youtube")) {
                    System.out.println("YouTube URL: " + mediaItem.get("url").getAsString());
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Funzione per eseguire una ricerca
    public static JsonObject searchSong(String query) throws IOException, InterruptedException {
        // URL di ricerca su Genius
        String url = "https://api.genius.com/search?q=" + query;

        // Crea un client HTTP
        HttpClient client = HttpClient.newHttpClient();

        // Crea la richiesta HTTP (GET) per la ricerca
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)  // Usa il token di accesso
                .build();

        // Invia la richiesta e ottieni la risposta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Se la richiesta è riuscita, restituire il risultato come JsonObject
        if (response.statusCode() == 200) {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } else {
            throw new IOException("Request failed with status code: " + response.statusCode());
        }
    }

    // Funzione per ottenere i dettagli della canzone
    public static JsonObject getSongDetails(String apiPath) throws IOException, InterruptedException {
        // URL per ottenere i dettagli della canzone
        String url = "https://api.genius.com" + apiPath;

        // Crea un client HTTP
        HttpClient client = HttpClient.newHttpClient();

        // Crea la richiesta HTTP (GET) per i dettagli della canzone
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)  // Usa il token di accesso
                .build();

        // Invia la richiesta e ottieni la risposta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Se la richiesta è riuscita, restituire il risultato come JsonObject
        if (response.statusCode() == 200) {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } else {
            throw new IOException("Request failed with status code: " + response.statusCode());
        }
    }
}
