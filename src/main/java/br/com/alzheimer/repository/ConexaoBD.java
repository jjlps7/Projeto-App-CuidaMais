package br.com.alzheimer.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexaoBD {
    private static final String URL = "jdbc:sqlite:assistente_alzheimer.db";

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void inicializarBanco() {
        String sqlUsuario = "CREATE TABLE IF NOT EXISTS usuario ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nome TEXT NOT NULL, "
                + "email TEXT NOT NULL UNIQUE, "
                + "senha_hash TEXT NOT NULL, "
                + "perfil TEXT NOT NULL, "
                + "cuidador_id INTEGER, "
                + "extra TEXT);";

        String sqlLembrete = "CREATE TABLE IF NOT EXISTS lembrete ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "titulo TEXT NOT NULL, "
                + "horario_programado TEXT NOT NULL, "
                + "caminho_audio_voz TEXT, "
                + "status TEXT NOT NULL, "
                + "cuidador_id INTEGER NOT NULL, "
                + "paciente_id INTEGER NOT NULL);";

        String sqlLocalizacao = "CREATE TABLE IF NOT EXISTS registro_localizacao ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "paciente_id INTEGER NOT NULL, "
                + "latitude REAL NOT NULL, "
                + "longitude REAL NOT NULL, "
                + "fora_zona_segura INTEGER NOT NULL, "
                + "registrado_em TEXT NOT NULL);";

        String sqlComportamento = "CREATE TABLE IF NOT EXISTS registro_comportamental ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "paciente_id INTEGER NOT NULL, "
                + "categoria TEXT NOT NULL, "
                + "intensidade INTEGER NOT NULL, "
                + "observacao TEXT, "
                + "registrado_em TEXT NOT NULL);";

        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsuario);
            stmt.execute(sqlLembrete);
            stmt.execute(sqlLocalizacao);
            stmt.execute(sqlComportamento);
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar banco SQLite: " + e.getMessage());
        }
    }
}
