package br.com.alzheimer.service;

import br.com.alzheimer.model.Cuidador;
import br.com.alzheimer.model.Paciente;
import br.com.alzheimer.model.ZonaSegura;
import br.com.alzheimer.repository.ConexaoBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class GeolocalizacaoService {
    public boolean verificarPosicao(Paciente paciente, Cuidador cuidador, ZonaSegura zona, double lat, double lon) {
        boolean dentro = zona.estaDentro(lat, lon);
        double distancia = zona.calcularDistanciaMetros(zona.getLatitudeCentro(), zona.getLongitudeCentro(), lat, lon);

        salvarHistorico(paciente.getId(), lat, lon, !dentro);

        if (!dentro) {
            cuidador.receberAlertaVermelho(String.format(
                    "Paciente %s saiu da zona segura '%s'! Distância atual: %.1f metros (limite: %.1f m).",
                    paciente.getNome(), zona.getNome(), distancia, zona.getRaioMetros()
            ));
        } else {
            System.out.printf("[GEO] Paciente %s dentro da zona '%s' (%.1f m do centro).%n",
                    paciente.getNome(), zona.getNome(), distancia);
        }
        return dentro;
    }

    private void salvarHistorico(int pacienteId, double lat, double lon, boolean foraZona) {
        String sql = "INSERT INTO registro_localizacao (paciente_id, latitude, longitude, fora_zona_segura, registrado_em) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pacienteId);
            ps.setDouble(2, lat);
            ps.setDouble(3, lon);
            ps.setInt(4, foraZona ? 1 : 0);
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao salvar localização: " + e.getMessage());
        }
    }
}
