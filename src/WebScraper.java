import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class WebScraper {

    // Funzione per fare lo scraping di canzoni, album e artisti su Genius
    public static void cercaSuWikipedia(String query, String tipoRicerca) throws IOException {
        String url = "https://it.wikipedia.org/wiki/" + query.replace(" ", "_");

        // Aggiungi l'header User-Agent per simulare una richiesta da browser
        Document doc = Jsoup.connect(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        Element primoParagrafo = doc.select("div.mw-parser-output > p").first();

        if (primoParagrafo != null) {
            // Stampa il contenuto del primo paragrafo (rimuovendo eventuali tag HTML)
            System.out.println(primoParagrafo.text());
        } else {
            System.out.println("Non è stato trovato un paragrafo iniziale nella pagina.");
        }
    }

    // Test della funzione di scraping
    public static void main(String[] args) throws IOException {
        // Esegui la ricerca per Artista
        cercaSuWikipedia("Adele", "artista");
    }
}
