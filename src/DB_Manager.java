import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class DB_Manager {
    private static final String url = "jdbc:mysql://localhost:3306/musicbuddy";
    private static final String user = "root";
    private static final String password = "";

    private Connection connection;

    public DB_Manager() {
        try {
            connection = DriverManager.getConnection(url, user, password);
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void insertArtista(String nome, String biografia) {
        String query = "INSERT INTO artista (nome, biografia) VALUES (?, ?)";

        try{
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, nome);
            statement.setString(2, biografia);
            statement.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void insertAlbum(String nome, Date dataUscita, String genere, int idArtista, String infoAlbum) {
        String query = "INSERT INTO album (nome, dataUscita, genere, idArtista, infoAlbum) VALUES (?, ?, ?, ?, ?)";

        try{
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, nome);
            statement.setDate(2, dataUscita);
            statement.setString(3, genere);
            statement.setInt(4, idArtista);
            statement.setString(5, infoAlbum);
            statement.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void insertCanzone(String titolo, String testo, Date dataUscita, int idAlbum, String linkSpotify, String linkYoutube, String infoCanzone) {
        String query = "INSERT INTO canzone (titolo, testo, dataUscita, idAlbum, linkSpotify, linkYoutube, infoCanzone) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try{
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, titolo);
            statement.setString(2, testo);
            statement.setDate(3, dataUscita);
            statement.setInt(4, idAlbum);
            statement.setString(5, linkSpotify);
            statement.setString(6, linkYoutube);
            statement.setString(7, infoCanzone);
            statement.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    //query per ottenere l'id dell'artista con quel nome
    public int getIdArtista(String artista){
        String query = "SELECT idArtista FROM artista WHERE nome = ?";
        int id = 0;

        try{
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, artista);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                id = rs.getInt("idArtista");
            }
        } catch(SQLException e){
            e.printStackTrace();
        }

        return id;
    }

    //query per ottenere l'id dell'album
    public int getIDAlbum(String nomeAlbum, String artista){
        String query = "SELECT al.idAlbum FROM album AS al " +
                "JOIN artista AS ar ON ar.idArtista = al.idArtista " +
                "WHERE al.nome = ? AND ar.nome = ?";

        int id = 0;

        try{
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, nomeAlbum);
            statement.setString(2, artista);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                id = rs.getInt("idAlbum");
            }
        } catch(SQLException e){
            e.printStackTrace();
        }

        return id;
    }


    public String getArtistaByNome(String nome) throws SQLException {
        String query = "SELECT * FROM Artista WHERE nome = ?";
        String artista = "";

        try(PreparedStatement statement = connection.prepareStatement(query)){
            statement.setString(1, nome);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String nomeArtista = resultSet.getString("nome");
                String biografia = resultSet.getString("biografia");

                artista = String.format("*Artista:* %s%n%n*Biografia*%n%s", nomeArtista, biografia);
            }
        }

        return artista;
    }


    public String getAlbumByNome(String nomeAlbum, String artista) throws SQLException {
        String query = "SELECT al.nome AS albumNome, al.dataUscita, al.genere, ar.nome AS artistaNome, al.infoAlbum " +
                "FROM album AS al " +
                "JOIN artista AS ar ON ar.idArtista = al.idArtista " +
                "WHERE al.nome = ? AND ar.nome = ?";

        String album = "";

        try(PreparedStatement statement = connection.prepareStatement(query)){
            statement.setString(1, nomeAlbum);
            statement.setString(2, artista);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String albumNome = resultSet.getString("albumNome");

                //formattazione della data
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                String dataUscita = dateFormat.format(resultSet.getDate("dataUscita"));

                String genere = resultSet.getString("genere");
                String artistaNome = resultSet.getString("artistaNome");
                String infoAlbum = resultSet.getString("infoAlbum");

                album = String.format("*Titolo:* %s%n" +
                                "*Artista:* %s%n" +
                                "*Data uscita:* %s%n" +
                                "*Genere:*%n%s%n%n" +
                                "*Dettagli*%n%s",
                        albumNome, artistaNome, dataUscita, genere, infoAlbum
                );
            }
        }

        return album;
    }


    public List<String> getCanzoneByNome(String titoloCanzone, String artista) throws SQLException {
        String query = "SELECT c.titolo, c.dataUscita, c.infoCanzone, c.linkSpotify, c.linkYoutube " +
                "FROM canzone AS c " +
                "JOIN album AS al ON c.idAlbum = al.idAlbum " +
                "JOIN artista AS ar ON al.idArtista = ar.idArtista " +
                "WHERE c.titolo LIKE ? AND ar.nome = ?";

        List<String> canzoneInfo = new ArrayList<>();

        try(PreparedStatement statement = connection.prepareStatement(query)){
            statement.setString(1, "%" + titoloCanzone + "%");
            statement.setString(2, artista);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String titolo = resultSet.getString("titolo");

                //formatta la data
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                String dataUscita = dateFormat.format(resultSet.getDate("dataUscita"));

                String infoCanzone = resultSet.getString("infoCanzone");

                //metto questo perchè nell'api di genius non tutte le canzoni hanno una sezione con descrizione/informazioni
                if(infoCanzone.equals("?")){
                    infoCanzone = "Informazioni sulla canzone non disponibili";
                }

                String canzone = String.format("*Titolo:* %s%n" +
                                "*Data uscita:* %s%n%n" +
                                "*Dettagli (in inglese)*%n%s",
                        titolo, dataUscita, infoCanzone
                );

                canzoneInfo.add(canzone);
                canzoneInfo.add(resultSet.getString("linkSpotify"));
                canzoneInfo.add(resultSet.getString("linkYoutube"));
            }
        }

        return canzoneInfo;
    }


    public String getTestoCanzone(String titoloCanzone, String artista) throws SQLException{
        String query = "SELECT c.titolo, c.testo " +
                "FROM canzone AS c " +
                "JOIN album AS al ON c.idAlbum = al.idAlbum " +
                "JOIN artista AS ar ON al.idArtista = ar.idArtista " +
                "WHERE c.titolo LIKE ? AND ar.nome = ?";

        String testo = "";

        try(PreparedStatement statement = connection.prepareStatement(query)){
            statement.setString(1, "%" + titoloCanzone + "%");
            statement.setString(2, artista);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()){
                String titolo = resultSet.getString("titolo");
                String txt = resultSet.getString("testo");

                testo = String.format("*Titolo:* %s%n%n" +
                        "*Testo* %n%s",

                        titolo, txt
                );
            }
        }

        return testo;
    }


    public List<String> getAlbumByDataUscita(String dataUscita) throws SQLException {
        String query = "SELECT al.nome AS albumNome, al.dataUscita, al.genere, ar.nome AS artistaNome, al.infoAlbum " +
                "FROM album AS al " +
                "JOIN artista AS ar ON ar.idArtista = al.idArtista " +
                "WHERE al.dataUscita = ?";

        List<String> albumInfo = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, dataUscita);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String albumNome = resultSet.getString("albumNome");

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                String dataUscitaFormatted = dateFormat.format(resultSet.getDate("dataUscita"));

                String genere = resultSet.getString("genere");
                String artistaNome = resultSet.getString("artistaNome");
                String infoAlbum = resultSet.getString("infoAlbum");

                String album = String.format("*Titolo:* %s%n" +
                                "*Artista:* %s%n" +
                                "*Data uscita:* %s%n" +
                                "*Genere:* %s%n%n" +
                                "*Dettagli*%n%s",
                        albumNome, artistaNome, dataUscitaFormatted, genere, infoAlbum);
                albumInfo.add(album);
            }
        }

        return albumInfo;
    }


    public List<String> getCanzoneByDataUscita(String dataUscita) throws SQLException {
        String query = "SELECT c.titolo, c.dataUscita, c.infoCanzone, c.linkSpotify, c.linkYoutube " +
                "FROM canzone AS c " +
                "JOIN album AS al ON c.idAlbum = al.idAlbum " +
                "JOIN artista AS ar ON al.idArtista = ar.idArtista " +
                "WHERE c.dataUscita = ?";

        List<String> canzoneInfo = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, dataUscita);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String titolo = resultSet.getString("titolo");

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                String dataUscitaFormatted = dateFormat.format(resultSet.getDate("dataUscita"));

                String infoCanzone = resultSet.getString("infoCanzone");

                //mettiamo questo perchè non tutte le canzoni hanno descrizioni
                if (infoCanzone.equals("?")) {
                    infoCanzone = "Informazioni sulla canzone non disponibili";
                }

                String canzone = String.format("*Titolo:* %s%n" +
                                "*Data uscita:* %s%n%n" +
                                "*Dettagli (in inglese)*%n%s",
                        titolo, dataUscitaFormatted, infoCanzone);
                canzoneInfo.add(canzone);
            }
        }

        return canzoneInfo;
    }
}