package br.com.alzheimer.repository;

import br.com.alzheimer.model.Lembrete;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LembreteDAO {
    public void salvar(Lembrete l) throws SQLException {
        String sql = "INSERT INTO lembrete (titulo, horario_programado, caminho_audio_voz, status, cuidador_id, paciente_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, l.getTitulo());
            ps.setString(2, l.getHorarioProgramado().toString());
            ps.setString(3, l.getCaminhoAudioVoz());
            ps.setString(4, l.getStatus().name());
            ps.setInt(5, l.getCuidador().getId());
            ps.setInt(6, l.getPaciente().getId());
            ps.executeUpdate();
        }
    }

    public List<String> listarResumo() throws SQLException {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT id, titulo, horario_programado, status FROM lembrete";
        try (Connection conn = ConexaoBD.conectar();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add("#" + rs.getInt("id") + " | " + rs.getString("horario_programado")
                        + " - " + rs.getString("titulo") + " [" + rs.getString("status") + "]");
            }
        }
        return lista;
    }
}
