import com.google.gson.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import java.io.IOException;

public class WebScraper {
    private static final String baseUrlWiki = "https://it.wikipedia.org";

    //metodo per fare lo scraping di un artista (cantante, band) su wikipedia
    public static void cercaArtista(String artista, String tipoRicerca) throws IOException {
        String url = baseUrlWiki + "/wiki/" + artista.replace(" ", "_");

        Document doc = Jsoup.connect(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        //controlla se la pagina è una disambiguazione
        if (isDisambiguationPage(doc)) {
            //trova il link più rilevante per il tipo di ricerca (artista, canzone, album)
            String relevantLink = findRelevantLink(doc, tipoRicerca);

            if (relevantLink != null) {
                //riprova lo scraping con il link specifico
                doc = Jsoup.connect(baseUrlWiki + relevantLink)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .get();
            }
        }

        String contenutoIntroduttivo = getContenutoIntroduttivo(doc);

        //stampa il contenuto introduttivo
        if (contenutoIntroduttivo.length() > 0) {
            //dbManager.insertArtista(artista, contenutoIntroduttivo.toString());

            cercaAlbum(doc, artista);
        }
        else{
            System.out.println("Non è stato trovato un contenuto introduttivo");
        }
    }

    //metodo per fare lo scraping degli album dell'artista su wikipedia
    private static void cercaAlbum(Document doc, String artista) throws IOException {
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
                    //chiama la funzione per cercare alcuni dettagli dell'album (data di uscita e genere)
                    System.out.println("ALBUM: " + nomeAlbum);
                    cercaDettagliAlbum(baseUrlWiki + linkAlbum, nomeAlbum, artista);
                    System.out.println("\n\n");
                }
            }
        }
    }

    //metodo per ottenere i dettagli di un album
    private static void cercaDettagliAlbum(String albumUrl, String nomeAlbum, String artista) throws IOException {
        Document albumDoc = Jsoup.connect(albumUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        //cerca la data completa di uscita
        Elements dataElement = albumDoc.select("tr:contains(Pubblicazione) td");
        String dataUscita = dataElement.isEmpty() ? "Data non trovata" : dataElement.first().text();

        //cerca il genere musicale
        Elements genereElement = albumDoc.select("tr:contains(Genere) td");
        String genere = genereElement.isEmpty() ? "Genere non trovato" : genereElement.first().text();

        //cerca info sull'album
        String info = getContenutoIntroduttivo(albumDoc);

        //inserisci i dettagli dell'album nel database
        //dbManager.insertAlbum(nomeAlbum, , genere, dbManager.gerArtistaByNome(artista), info);


        //CERCA ELENCO TRACCE
        //trova il div che contiene la sezione "Tracce"
        Element divContent = albumDoc.selectFirst("div.mw-content-ltr.mw-parser-output");

        if (divContent == null) {
            System.out.println("Non è stato trovato il div principale della pagina");
            return;
        }

        //trova il tag h2#Tracce
        Element sezioneTracce = divContent.selectFirst("h2#Tracce");

        if (sezioneTracce == null) {
            System.out.println("Nessuna sezione 'Tracce' trovata");
            return;
        }

        //seleziona il primo <ol> che segue il div che contiente l'elemento h2#Tracce
        Element albumList = sezioneTracce.parent().nextElementSibling();

        //naviga attraverso gli elementi successivi fino a trovare il primo <ol>
        while (albumList != null && !albumList.tagName().equals("ol")) {
            albumList = albumList.nextElementSibling();
        }

        while (albumList != null) {
            if (albumList.tagName().equals("ol")) {     //se l'elemento è un ol, estrae i titoli delle tracce
                Elements tracce = albumList.select("li i");

                for (Element traccia : tracce) {
                    System.out.println("Titolo traccia: " + traccia.text());

                    cercaInfoCanzone(traccia.text(), artista);
                    System.out.println("\n");
                }
            }
            else if (albumList.tagName().equals("dl")) {
                Elements dtElements = albumList.select("dt");
                boolean contieneDiscoOLato = dtElements.stream()
                        .anyMatch(dt -> dt.text().toLowerCase().contains("disco") || dt.text().toLowerCase().contains("lato"));

                if (!contieneDiscoOLato) {  //se il <dl> non contiene "disco" o "lato", interrompe il ciclo
                    break;
                }
            }
            else {  //se è qualsiasi altro elemento, interrompe il ciclo
                break;
            }

            //passa all'elemento successivo
            albumList = albumList.nextElementSibling();
        }
    }

    private static void cercaInfoCanzone(String titolo, String artista) throws IOException {
        String query = titolo.replace(" ", "%20") + "%20" + artista.replace(" ", "%20");
        JsonObject searchResult = APIGenius.cercaCanzoneGenius(query);

        if (searchResult == null) {
            System.out.println("Nessun risultato trovato per la canzone: " + titolo);
            return;
        }

        //prende il primo elemento hit dal risultato della ricerca (hit sono vari risultati possibili della ricerca)
        JsonObject firstHit = searchResult.getAsJsonObject("response")
                .getAsJsonArray("hits")
                .get(0).getAsJsonObject()
                .getAsJsonObject("result");

        //ottiene l'API path per ottenere i dettagli della canzone
        String apiPath = firstHit.get("api_path").getAsString();
        String[] dettagli = APIGenius.getDettagli(apiPath);

        String spotifyLink = (dettagli[0] != null ? dettagli[0] : "Non disponibile");
        String youtubeLink = (dettagli[1] != null ? dettagli[1] : "Non disponibile");
        String lyricsURL = (dettagli[2] != null ? "https://genius.com" + dettagli[2] : "Non disponibile");
        String descrizione = (dettagli[3] != null ? dettagli[3] : "Non disponibile");

        String testo = (!lyricsURL.equals("Non disponibile") ? scrapeTesto(lyricsURL) : "Non disponibile");

        if(testo.equals("")){
            testo = "Non disponibile";
        }


        // Inserimento nel database (da implementare)
        // dbManager.insertCanzone(titolo, testo, , spotifyUrl, youtubeUrl);
    }

    //metodo per fare lo scraping del testo della canzone
    private static String scrapeTesto(String lyricsURL) throws IOException {
        Document doc = Jsoup.connect(lyricsURL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        StringBuilder testo = new StringBuilder();

        //seleziona tutti gli elementi che contengono il testo della canzone
        Elements lyricElements = doc.select("[data-lyrics-container=\"true\"]");

        for (Element el : lyricElements) {
            //analizza ricorsivamente ogni nodo all'interno dell'elemento che contiene il testo
            analizzaNodi(el, testo);
            testo.append("\n");
        }

        return testo.toString().trim();
    }

    //metodo ricorsivo per analizzare i nodi
    private static void analizzaNodi(Node node, StringBuilder testo) {
        if (node instanceof TextNode) {     //se è un nodo di tipo testuale puro, si aggiunge il contenuto al testo
            testo.append(((TextNode) node).text());
        } else if (node instanceof Element) {
            Element element = (Element) node;

            if (element.tagName().equals("br")) {   //se è un elemento <br> viene aggiunto un "\n"
                testo.append("\n");
            } else {
                //analizza i figli ricorsivamente, in questo modo viene letta tutta la struttura completa dell'html complesso
                //senza tralasciare alcun elemento
                for (Node child : element.childNodes()) {
                    analizzaNodi(child, testo);
                }
            }
        }
    }

    //metodo che permette di ottenere l'introduzione della pagina wikipedia desiderata
    private static String getContenutoIntroduttivo(Document doc){
        //ottiene il contenuto introduttivo fino alla sezione "biografia" oppure "storia"
        Elements elementi = doc.select("div.mw-content-ltr.mw-parser-output").first().children(); //ottiene tutti i figli della sezione principale

        StringBuilder contenuto = new StringBuilder();

        //raccoglie dati specifici per artista/canzone/album
        for (Element elemento : elementi) {
            //fa lo scraping fino alla sezione "Biografia"
            if (elemento.tagName().equals("div") && elemento.hasClass("mw-heading2")) {
                break;
            }
            else if (elemento.tagName().equals("p")) {
                String testoParagrafo = rimuoviNumeriTraParentesi(elemento.text());
                contenuto.append(testoParagrafo).append("\n");
            }
        }

        return contenuto.toString();
    }

    //metodo per rimuovere informazioni aggiuntive di wikipedia
    private static String rimuoviNumeriTraParentesi(String testo) {
        testo = testo.replaceAll("\\[.*?\\]", "");  //rimuove i numeri tra parentesi quadre
        testo = testo.replaceAll("AFI:\\s*;\\s*", "");  //rimuove le pronunce in alfabeto fonetico internazionale
        return testo;
    }

    //metodo per verificare se la pagina è una disambiguazione
    private static boolean isDisambiguationPage(Document doc) {
        return !doc.select("div.avviso-disambigua").isEmpty() ||
                !doc.select("table.disambiguation").isEmpty() ||
                doc.title().toLowerCase().contains("disambiguazione");
    }

    //metodo per trovare il link più rilevante nella pagina di disambiguazione
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
            //controlla se il link ha l'attributo title e se contiene il testo di ricerca
            String title = link.attr("title");

            if (!title.isEmpty() && title.toLowerCase().contains(ricercaTitolo.toLowerCase())) {
                return link.attr("href");
            }
        }

        return null;
    }

    // TEST
    public static void main(String[] args) throws IOException {

        try {
            //DB_Manager dbManager = new DB_Manager();

            cercaArtista("Pink Floyd", "artista");

            //dbManager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*
        try {
            //DB_Manager dbManager = new DB_Manager();

            cercaArtista(dbManager, "Pink Floyd", "artista");

            //dbManager.close();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }

         */
    }
}
