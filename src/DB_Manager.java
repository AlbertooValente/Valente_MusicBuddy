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

        try (PreparedStatement statement = connection.prepareStatement(query)) {
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

        try (PreparedStatement statement = connection.prepareStatement(query)) {
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

        try (PreparedStatement statement = connection.prepareStatement(query)) {
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

        try (PreparedStatement statement = connection.prepareStatement(query)) {
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

        try (PreparedStatement statement = connection.prepareStatement(query)) {
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
        String artista = null;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, nome);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String nomeArtista = resultSet.getString("nome");
                    String biografia = resultSet.getString("biografia");

                    artista = String.format("*Artista:* %s%n%n*Biografia*%n%s", nomeArtista, biografia);
                }
            }
        }

        return artista;
    }


    public String getAlbumByNome(String nomeAlbum, String artista) throws SQLException {
        String sql = "SELECT al.nome AS albumNome, al.dataUscita, al.genere, ar.nome AS artistaNome, al.infoAlbum " +
                "FROM album AS al " +
                "JOIN artista AS ar ON ar.idArtista = al.idArtista " +
                "WHERE al.nome = ? AND ar.nome = ?";

        String album = null;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nomeAlbum);
            statement.setString(2, artista);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String albumNome = resultSet.getString("albumNome");

                    //formattazione della data
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    String dataUscita = dateFormat.format(resultSet.getDate("dataUscita"));

                    String genere = resultSet.getString("genere");
                    String artistaNome = resultSet.getString("artistaNome");
                    String infoAlbum = resultSet.getString("infoAlbum");

                    album = String.format(
                            "*Titolo:* %s%n" +
                            "*Artista:* %s%n" +
                            "*Data uscita:* %s%n" +
                            "*Genere:*%n%s%n%n" +
                            "*Dettagli*%n%s",
                            albumNome, artistaNome, dataUscita, genere, infoAlbum
                    );
                }
            }
        }

        return album;
    }

    public List<String> getCanzoneByNome(String nomeCanzone, String artista) throws SQLException {
        String query = "SELECT c.titolo, c.dataUscita, c.infoCanzone, c.linkSpotify, c.linkYoutube " +
                "FROM canzone AS c " +
                "JOIN album AS al ON c.idAlbum = al.idAlbum " +
                "JOIN artista AS ar ON al.idArtista = ar.idArtista" +
                "WHERE c.titolo = ? AND ar.nome = ?";

        List<String> canzoneInfo = null;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, nomeCanzone);
            statement.setString(2, artista);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String titolo = (resultSet.getString("titolo"));

                    //formatta la data
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    String dataUscita = dateFormat.format(resultSet.getDate("dataUscita"));

                    String infoCanzone = resultSet.getString("infoCanzone");

                    String canzone = String.format(
                            "*Titolo:* %s%n" +
                                    "*Data uscita:* %s%n%n" +
                                    "*Dettagli:*%n%s",
                            titolo, dataUscita, infoCanzone
                    );

                    canzoneInfo.add(canzone);
                    canzoneInfo.add(resultSet.getString("linkSpotify"));
                    canzoneInfo.add(resultSet.getString("linkYoutube"));
                }
            }
        }

        return canzoneInfo;
    }

    //temporanea
    public ArrayList<String> getAlbumsByArtista(int idArtista) throws SQLException {
        String sql = "SELECT * FROM Album WHERE idArtista = ?";
        ArrayList<String> albums = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idArtista);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    albums.add(resultSet.getString("nome") + " - " + resultSet.getString("genere"));
                }
            }
        }

        return albums;
    }

    //temporanea
    public ArrayList<String> getCanzoniByAlbum(int idAlbum) throws SQLException {
        String sql = "SELECT * FROM Canzone WHERE idAlbum = ?";
        ArrayList<String> canzoni = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idAlbum);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    canzoni.add(resultSet.getString("titolo") + " - " + resultSet.getString("genere"));
                }
            }
        }

        return canzoni;
    }

    //temporanea
    public ArrayList<String> getCanzoniByArtista(int idArtista) throws SQLException {
        String sql = "SELECT c.titolo, c.genere, c.dataUscita, c.durata " +
                "FROM Canzone c " +
                "JOIN Album a ON c.idAlbum = a.idAlbum " +
                "WHERE a.idArtista = ?";

        ArrayList<String> canzoni = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idArtista);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String titolo = resultSet.getString("titolo");
                    String genere = resultSet.getString("genere");
                    Date dataUscita = resultSet.getDate("dataUscita");
                    Time durata = resultSet.getTime("durata");
                    canzoni.add(titolo + " - " + genere + " - " + dataUscita + " - " + durata);
                }
            }
        }

        return canzoni;
    }

    //chiude la connessione
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}