import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class APISpotify {
    private static final String accessToken = Config.getAPISpotifyToken();
    private static final HttpClient client = HttpClient.newHttpClient();

    // Metodo per cercare le playlist in base al genere
    public static JsonObject searchTracks(String genre) {
        try {
            // Costruzione URL per la ricerca di canzoni
            String url = "https://api.spotify.com/v1/search?q=genre:" + genre + "&type=track&limit=10";

            // Creazione della richiesta
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .build();

            // Invio della richiesta e gestione della risposta
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                return jsonResponse;
            } else {
                System.out.println("Errore durante la richiesta a Spotify. Codice di stato: " + response.statusCode());
                System.out.println("Dettaglio: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Errore durante la richiesta a Spotify: " + e.getMessage());
        }
        return null; // Ritorna null in caso di errore
    }

    public static void main(String[] args) {
        // Eseguiamo la ricerca di 10 canzoni nel genere "pop"
        JsonObject tracks = searchTracks("ambient_techno");

        if (tracks != null && tracks.has("tracks")) {
            tracks.getAsJsonObject("tracks").getAsJsonArray("items").forEach(track -> {

                if (track != null && track.isJsonObject()) { // Verifica che l'elemento sia valido
                    JsonObject trackObj = track.getAsJsonObject();
                    if (trackObj.has("name") && trackObj.has("artists") && trackObj.has("external_urls")) {
                        String trackName = trackObj.get("name").getAsString();
                        String artistName = trackObj.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
                        String url = trackObj.getAsJsonObject("external_urls").get("spotify").getAsString();
                        System.out.println("Canzone: " + trackName + " | Artista: " + artistName + " | URL: " + url);
                    } else {
                        System.out.println("Dati della canzone mancanti o non validi.");
                    }
                } else {
                    System.out.println("Elemento canzone non valido o null.");
                }
            });
        } else {
            System.out.println("Nessuna canzone trovata o errore nella risposta.");
        }
    }

    /*
    LISTA
    Pop, Rock, Hip-hop, Indie, Jazz, Classical, Electronic, Reggaeton, Blues, Country, Soul, Funk, R&B, Dance, Metal, Punk, Alternative, Latin, Disco, Trap, Folk, Ambient, Techno, House, EDM, Grunge, Experimental, Ska, Pop Rock, Dubstep, Reggae, K-Pop, Vaporwave, Chill, Tropical, Acoustic, Hard Rock, Trance, Post-rock, New Age, Disco, Garage, Synthwave, Gothic, Post-punk, Indie Pop, Indie Rock, Alternative Rock, Electropop, Lo-fi Hip-hop, Progressive Rock, Ambient Techno, Deep House, Future Bass, Tech House, Electro Swing, Chillwave, Pop Punk, Shoegaze, Post-punk revival, Neo-soul, Acid Jazz, Afrobeat, Acid Rock, Bluegrass, Bossa Nova, Chiptune, Cumbia, Disco Funk, Hardcore, Jazz Fusion, Math Rock, Post-hardcore, Trap Latino, Salsa, Tejano, Tango
     */
}
