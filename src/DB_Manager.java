import java.sql.*;
import java.util.ArrayList;

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
        String sql = "INSERT INTO artista (nome, biografia) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nome);
            statement.setString(2, biografia);
            statement.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void insertAlbum(String nome, Date dataUscita, String genere, int idArtista, String infoAlbum) {
        String sql = "INSERT INTO album (nome, dataUscita, genere, idArtista, infoAlbum) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
        String sql = "INSERT INTO canzone (titolo, testo, dataUscita, idAlbum, linkSpotify, linkYoutube, infoCanzone) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
        String sql = "SELECT idArtista FROM artista WHERE nome = ?";
        int id = 0;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
        String sql = "SELECT al.idAlbum FROM album AS al " +
                "JOIN artista AS ar ON ar.idArtista = al.idArtista " +
                "WHERE al.nome = ? AND ar.nome = ?";

        int id = 0;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
        String sql = "SELECT * FROM Artista WHERE nome = ?";
        String artista = null;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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

    //fallo by nome e artista
    public String getAlbumByNome(String nomeAlbum) throws SQLException {
        String sql = "SELECT * FROM Album WHERE nome = ?";
        String album = null;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nomeAlbum);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    album = resultSet.getString("nome") + " - " + resultSet.getString("genere") + " - " + resultSet.getDate("dataUscita");
                }
            }
        }

        return album;
    }

    public String getCanzoneByNome(String nomeCanzone) throws SQLException {
        String sql = "SELECT * FROM Canzone WHERE titolo = ?";
        String canzone = null;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nomeCanzone);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    canzone = resultSet.getString("titolo") + " - " + resultSet.getString("genere") + " - " + resultSet.getDate("dataUscita") + " - " + resultSet.getTime("durata");
                }
            }
        }

        return canzone;
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