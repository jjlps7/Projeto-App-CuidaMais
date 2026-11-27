package br.com.alzheimer.app;

import br.com.alzheimer.model.*;
import br.com.alzheimer.repository.ConexaoBD;
import br.com.alzheimer.repository.LembreteDAO;
import br.com.alzheimer.service.GeolocalizacaoService;
import br.com.alzheimer.service.MonitoramentoService;

import java.time.LocalTime;

public class Main {
    public static void main(String[] args) throws Exception {
        ConexaoBD.inicializarBanco();
        LembreteDAO lembreteDAO = new LembreteDAO();
        GeolocalizacaoService geoService = new GeolocalizacaoService();
        MonitoramentoService monitoramentoService = new MonitoramentoService();

        Cuidador cuidador = new Cuidador(1, "João (Filho)", "joao@email.com", "1234", "(82) 99999-0000");
        Paciente paciente = new Paciente(2, "Carlos (Pai)", "carlos@email.com", "0000", 1, "MODERADO");

        System.out.println("=== ASSISTENTE DE ROTINA - ALZHEIMER ===");
        System.out.println("Login Cuidador autenticado: " + cuidador.autenticar("1234"));

        Lembrete lembreteRemedio = new Lembrete(
                101,
                "Tomar remédio para pressão",
                LocalTime.of(8, 0),
                "audio_filho_remedio_azul.mp3",
                cuidador,
                paciente
        );
        lembreteRemedio.dispararNotificacaoContextual();
        lembreteRemedio.confirmarRealizacao();
        lembreteDAO.salvar(lembreteRemedio);

        System.out.println("\n--- SIMULAÇÃO DE ESCALONAMENTO (FA01) ---");
        Lembrete lembreteBanho = new Lembrete(
                102,
                "Banho da manhã",
                LocalTime.of(9, 0),
                "audio_filho_banho.mp3",
                cuidador,
                paciente
        );
        lembreteBanho.dispararNotificacaoContextual();
        lembreteBanho.checarTimeoutEEscalonar(15);
        lembreteDAO.salvar(lembreteBanho);

        System.out.println("\n--- MONITORAMENTO DE GEOLOCALIZAÇÃO ---");
        ZonaSegura casa = new ZonaSegura(1, paciente.getId(), "Residência da Família", -9.6658, -35.7350, 150.0);
        geoService.verificarPosicao(paciente, cuidador, casa, -9.6659, -35.7351);
        geoService.verificarPosicao(paciente, cuidador, casa, -9.6700, -35.7400);

        System.out.println("\n--- MONITORAMENTO COMPORTAMENTAL ---");
        RegistroComportamental reg = new RegistroComportamental(
                1, paciente.getId(), "AGITACAO_NOTURNA", 4, "Acordou desorientado às 02:30."
        );
        monitoramentoService.registrarOcorrencia(paciente, cuidador, reg);

        System.out.println("\n--- HISTÓRICO PERSISTIDO NO BANCO (SQLite) ---");
        for (String item : lembreteDAO.listarResumo()) {
            System.out.println(item);
        }
    }
}
