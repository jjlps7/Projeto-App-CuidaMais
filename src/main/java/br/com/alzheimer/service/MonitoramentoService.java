package br.com.alzheimer.service;

import br.com.alzheimer.model.Cuidador;
import br.com.alzheimer.model.Paciente;
import br.com.alzheimer.model.RegistroComportamental;
import br.com.alzheimer.repository.ConexaoBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MonitoramentoService {
    private final List<RegistroComportamental> registrosEmMemoria = new ArrayList<>();

    public void registrarOcorrencia(Paciente paciente, Cuidador cuidador, RegistroComportamental registro) {
        registrosEmMemoria.add(registro);
        salvarNoBanco(registro);

        System.out.println("[COMPORTAMENTO] Registrado: " + registro.getCategoria()
                + " | Intensidade: " + registro.getIntensidade() + "/5");

        if (registro.isCritico()) {
            cuidador.receberAlertaVermelho("Ocorrência comportamental crítica (" + registro.getCategoria()
                    + " - nível " + registro.getIntensidade() + "/5) para " + paciente.getNome());
        }
    }

    private void salvarNoBanco(RegistroComportamental r) {
        String sql = "INSERT INTO registro_comportamental (paciente_id, categoria, intensidade, observacao, registrado_em) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBD.conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getPacienteId());
            ps.setString(2, r.getCategoria());
            ps.setInt(3, r.getIntensidade());
            ps.setString(4, r.getObservacao());
            ps.setString(5, r.getRegistradoEm().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao salvar registro comportamental: " + e.getMessage());
        }
    }

    public List<RegistroComportamental> getRegistros() {
        return registrosEmMemoria;
    }
}
