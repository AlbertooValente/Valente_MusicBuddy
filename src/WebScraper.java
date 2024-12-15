import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.sql.SQLException;

public class WebScraper {
    //funzione per fare lo scraping di un artista (cantante, band) su wikipedia
    public static void cercaArtista(String artista, String tipoRicerca) throws IOException, SQLException {
        String baseUrl = "https://it.wikipedia.org";
        String url = baseUrl + "/wiki/" + artista.replace(" ", "_");

        Document doc = Jsoup.connect(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        //controlla se la pagina è una disambiguazione
        if (isDisambiguationPage(doc)) {
            //trova il link più rilevante per il tipo di ricerca (artista, canzone, album)
            String relevantLink = findRelevantLink(doc, tipoRicerca);

            if (relevantLink != null) {
                //riprova lo scraping con il link specifico
                doc = Jsoup.connect(baseUrl + relevantLink)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .get();
            }
            /*
            MESSAGGIO ERRORE ???
             */
        }

        //ottiene il contenuto introduttivo fino alla sezione "biografia" oppure "storia"
        Elements elementi = doc.select("div.mw-content-ltr.mw-parser-output").first().children(); //ottiene tutti i figli della sezione principale

        StringBuilder contenutoIntroduttivo = new StringBuilder();

        //raccoglie dati specifici per artista/canzone/album
        for (Element elemento : elementi) {
            //fa lo scraping fino alla sezione "Biografia"
            if (elemento.tagName().equals("div") && elemento.hasClass("mw-heading2") && (elemento.text().toLowerCase().contains("biografia")
            || elemento.text().toLowerCase().contains("storia"))) {
                break;
            }
            else if (elemento.tagName().equals("p")) {
                String testoParagrafo = rimuoviNumeriTraParentesi(elemento.text());
                contenutoIntroduttivo.append(testoParagrafo).append("\n");
            }
        }

        //stampa il contenuto introduttivo
        if (contenutoIntroduttivo.length() > 0) {
            //dbManager.insertArtista(artista, contenutoIntroduttivo.toString());
            cercaAlbum(artista);
        }
        else{
            System.out.println("Non è stato trovato un contenuto introduttivo");
        }
    }

    //funzione per fare lo scraping degli album dell'artista su wikipedia
    public static void cercaAlbum(String artista) throws IOException, SQLException {
        String baseUrl = "https://it.wikipedia.org";
        String url = baseUrl + "/wiki/" + artista.replace(" ", "_");

        Document doc = Jsoup.connect(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        //trova il div che contiene la sezione "Album in studio" o "Discografia"
        Element divContent = doc.selectFirst("div.mw-content-ltr.mw-parser-output");

        if (divContent == null) {
            System.out.println("Non è stato trovato il div principale della pagina");
            return;
        }

        //trova il tag h3#Album_in_studio o h2#Discografia
        Element sezioneAlbum = divContent.selectFirst("h3#Album_in_studio");

        if (sezioneAlbum == null) {
            sezioneAlbum = divContent.selectFirst("h2#Discografia");

            if (sezioneAlbum == null) {
                System.out.println("Nessuna sezione 'Album in studio' o 'Discografia' trovata");
                return;
            }
        }

        //seleziona il primo <ul> che segue il div che contiente l'elemento h3#Album_in_studio o h2#Discografia
        Element albumList = sezioneAlbum.parent().nextElementSibling();

        //naviga attraverso gli elementi successivi fino a trovare il primo <ul>
        while (albumList != null && !albumList.tagName().equals("ul")) {
            albumList = albumList.nextElementSibling();
        }

        //estrae gli album dal primo <ul> trovato
        if (albumList != null) {
            Elements albumLinks = albumList.select("li i a");   //seleziona i link degli album

            for (Element albumLink : albumLinks) {
                String nomeAlbum = albumLink.text();
                String linkAlbum = albumLink.attr("href");

                //stampa i dettagli dell'album
                if (nomeAlbum != null && !linkAlbum.isEmpty()) {
                    System.out.println("Link album: " + linkAlbum);

                    //chiama la funzione per cercare alcuni dettagli dell'album (data di uscita e genere)
                    cercaDettagliAlbum(baseUrl + linkAlbum, nomeAlbum);

                    // (Devi definire una funzione che prenda il nome dell'album e vada su Genius per estrarre le info)
                }
            }
        }
    }

    //funzione per ottenere i dettagli di un album
    public static void cercaDettagliAlbum(String albumUrl, String nomeAlbum) throws IOException, SQLException {
        Document albumDoc = Jsoup.connect(albumUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        //cerca la data completa di uscita
        Elements dataElement = albumDoc.select("tr:contains(Pubblicazione) td");
        String dataUscita = dataElement.isEmpty() ? "Data non trovata" : dataElement.first().text();

        //cerca il genere musicale
        Elements genereElement = albumDoc.select("tr:contains(Genere) td");
        String genere = genereElement.isEmpty() ? "Genere non trovato" : genereElement.first().text();

        System.out.println("Album: " + nomeAlbum);
        System.out.println("Data di uscita: " + dataUscita);
        System.out.println("Genere: " + genere + "\n");

        //inserisci i dettagli dell'album nel database
        //dbManager.insertAlbum(nomeAlbum, java.sql.Date.valueOf(formattaData(dataUscita)), genere, dbManager.getArtistaIdByName(artista), "", "", "");
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
    private static String findRelevantLink(Document doc, String tipoRicerca) {
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
        try{
            cercaArtista("Pink Floyd", "artista");
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }

        /*
        try {
            DB_Manager dbManager = new DB_Manager();

            cercaArtista(dbManager, "Pink Floyd", "artista");

            dbManager.close();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
         */
    }
}
