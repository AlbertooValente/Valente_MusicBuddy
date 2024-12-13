import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;

public class WebScraper {
    //funzione per fare lo scraping di canzoni, album e artisti su wikipedia
    public static void cercaSuWikipedia(String query, String tipoRicerca, String artista) throws IOException {
        String baseUrl = "https://it.wikipedia.org";
        String url = baseUrl + "/wiki/" + query.replace(" ", "_");

        Document doc = Jsoup.connect(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        //controlla se la pagina è una disambiguazione
        if (isDisambiguationPage(doc)) {
            //trova il link più rilevante per il tipo di ricerca (artista, canzone, album)
            String relevantLink = findRelevantLink(doc, tipoRicerca, artista);

            if (relevantLink != null) {
                //riprova lo scraping con il link specifico
                doc = Jsoup.connect(baseUrl + relevantLink)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .get();
            } else {
                System.out.println("Nessun link rilevante trovato per " + tipoRicerca);
                return;
            }
        }

        //ottiene il contenuto introduttivo fino alla sezione "Biografia"
        Elements elementi = doc.select("div.mw-parser-output").first().children(); //ottiene tutti i figli della sezione principale

        StringBuilder contenutoIntroduttivo = new StringBuilder();

        //raccoglie dati specifici per artista/canzone/album
        for (Element elemento : elementi) {
            if (tipoRicerca.equals("artista")) {
                //se è un artista, fa lo scraping fino alla sezione "Biografia"
                if (elemento.tagName().equals("div") && elemento.hasClass("mw-heading2") && elemento.text().toLowerCase().contains("biografia")) {
                    break;
                } else if (elemento.tagName().equals("p")) {
                    String testoParagrafo = rimuoviNumeriTraParentesi(elemento.text());
                    contenutoIntroduttivo.append(testoParagrafo).append("\n");
                }
            } else {
                //per album o canzone, raccoglie paragrafi relativi a descrizione, anno, ecc.
                if (elemento.tagName().equals("p")) {
                    String testoParagrafo = rimuoviNumeriTraParentesi(elemento.text());
                    contenutoIntroduttivo.append(testoParagrafo).append("\n");
                }
            }
        }

        //stampa il contenuto introduttivo
        if (contenutoIntroduttivo.length() > 0) {
            System.out.println("Risultato:\n" + contenutoIntroduttivo.toString());
        } else {
            System.out.println("Non è stato trovato un contenuto introduttivo nella pagina.");
        }
    }

    //funzione per rimuovere informazioni aggiuntive di wikipedia
    private static String rimuoviNumeriTraParentesi(String testo) {
        testo = testo.replaceAll("\\[.*?\\]", "");  //rimuove i numeri tra parentesi quadre
        testo = testo.replaceAll("AFI:\\s*;\\s*", "");  //rimuove le pronunce in alfabeto fonetico internazionale
        return testo;
    }

    //funzione per verificare se la pagina è una disambiguazione
    private static boolean isDisambiguationPage(Document doc) {
        return !doc.select("div.avviso-disambigua").isEmpty() ||
                !doc.select("table.disambiguation").isEmpty() ||
                doc.title().toLowerCase().contains("disambiguazione");
    }

    //funzione per trovare il link più rilevante nella pagina di disambiguazione
    private static String findRelevantLink(Document doc, String tipoRicerca, String artista) {
        Elements links = doc.select("div.mw-parser-output a");

        String ricercaTitolo = "";

        if (tipoRicerca.equalsIgnoreCase("artista")) {
            ricercaTitolo = "cantante";
        } else if (tipoRicerca.equalsIgnoreCase("album")) {
            ricercaTitolo = "album";
        } else if (tipoRicerca.equalsIgnoreCase("canzone")) {
            ricercaTitolo = "brano musicale";
        }

        for (Element link : links) {
            // Controlla se il link ha l'attributo title e se contiene il testo di ricerca
            String title = link.attr("title");

            if (!title.isEmpty() && title.toLowerCase().contains(ricercaTitolo.toLowerCase())) {
                return link.attr("href");  // Restituisci il link relativo trovato
            }
        }

        return null;
    }

    // TEST
    public static void main(String[] args) throws IOException {
        cercaSuWikipedia("Adele", "artista", "");

        /*

        cercaSuWikipedia("Wish you were here", "canzone", "Pink Floyd");
        System.out.println("\n\n\n");
        cercaSuWikipedia("Wish you were here", "album", "Pink Floyd");

        */
    }
}
