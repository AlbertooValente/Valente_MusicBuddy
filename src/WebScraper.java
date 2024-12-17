import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.sql.SQLException;

public class WebScraper {
    private static final String baseUrlWiki = "https://it.wikipedia.org";

    //funzione per fare lo scraping di un artista (cantante, band) su wikipedia
    public static void cercaArtista(String artista, String tipoRicerca) throws IOException, SQLException {
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

    //funzione per fare lo scraping degli album dell'artista su wikipedia
    private static void cercaAlbum(Document doc, String artista) throws IOException, SQLException {
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
                    cercaDettagliAlbum(baseUrlWiki + linkAlbum, nomeAlbum, artista);
                }
            }
        }
    }

    //funzione per ottenere i dettagli di un album
    private static void cercaDettagliAlbum(String albumUrl, String nomeAlbum, String artista) throws IOException, SQLException {
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
            System.out.println("Nessuna sezione 'Album in studio' o 'Discografia' trovata");
            return;
        }

        //seleziona il primo <ul> che segue il div che contiente l'elemento h2#Tracce
        Element albumList = sezioneTracce.parent().nextElementSibling();

        while (albumList != null) {
            // Se troviamo un elemento dl, controlliamo se contiene la parola "disco"
            if (albumList.tagName().equals("dl")) {
                String dlText = albumList.text().toLowerCase();
                if (dlText.contains("disco")) {
                    // Se c'è la parola "disco", continuiamo a cercare
                    System.out.println("Sezione Disco trovata, continuando a leggere...");
                } else {
                    // Se il dl non contiene "disco", interrompiamo
                    break;
                }
            }

            // Se l'elemento è un ul, estrai i titoli delle tracce
            if (albumList.tagName().equals("ul")) {
                Elements tracce = albumList.select("li");
                for (Element traccia : tracce) {
                    System.out.println("Titolo traccia: " + traccia.text());
                }
            }

            // Passa all'elemento successivo
            albumList = albumList.nextElementSibling();
        }


        //cercaCanzoneGenius(nomeAlbum, artista);
    }

    private static void cercaCanzoneGenius(String nomeAlbum, String artista) throws IOException, SQLException {


    }

    //funzione che permette di ottenere l'introduzione della pagina wikipedia desiderata
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
        } catch (IOException | SQLException e) {
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
