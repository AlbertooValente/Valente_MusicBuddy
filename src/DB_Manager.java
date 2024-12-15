import java.sql.*;
import java.util.ArrayList;

public class DB_Manager {
    private static final String url = "jdbc:mysql://localhost:3306/musicbuddy";
    private static final String user = "root";
    private static final String password = "";

    private Connection connection;

    public DB_Manager() throws SQLException {
        connection = DriverManager.getConnection(url, user, password);
    }

    public void insertArtista(String nome, String biografia) throws SQLException {
        String sql = "INSERT INTO Artista (nome, biografia) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nome);
            statement.setString(2, biografia);
            statement.executeUpdate();
        }
    }

    public void insertAlbum(String nome, Date dataUscita, String genere, int idArtista, String linkSpotify, String linkYoutube, String infoAlbum) throws SQLException {
        String sql = "INSERT INTO Album (nome, dataUscita, genere, idArtista, linkSpotify, linkYoutube, infoAlbum) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nome);
            statement.setDate(2, dataUscita);
            statement.setString(3, genere);
            statement.setInt(4, idArtista);
            statement.setString(5, linkSpotify);
            statement.setString(6, linkYoutube);
            statement.setString(7, infoAlbum);
            statement.executeUpdate();
        }
    }

    public void insertCanzone(String titolo, String testo, Date dataUscita, String genere, Time durata, int idAlbum, String linkSpotify, String linkYoutube, String infoCanzone) throws SQLException {
        String sql = "INSERT INTO Canzone (titolo, testo, dataUscita, genere, durata, idAlbum, linkSpotify, linkYoutube, infoCanzone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, titolo);
            statement.setString(2, testo);
            statement.setDate(3, dataUscita);
            statement.setString(4, genere);
            statement.setTime(5, durata);
            statement.setInt(6, idAlbum);
            statement.setString(7, linkSpotify);
            statement.setString(8, linkYoutube);
            statement.setString(9, infoCanzone);
            statement.executeUpdate();
        }
    }

    public String getArtistaByNome(String nome) throws SQLException {
        String sql = "SELECT * FROM Artista WHERE nome = ?";
        String artista = null;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nome);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    artista = resultSet.getString("nome") + " - " + resultSet.getString("biografia");
                }
            }
        }

        return artista;
    }

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