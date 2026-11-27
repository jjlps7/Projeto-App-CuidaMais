package br.com.alzheimer.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class Lembrete {
    private int id;
    private String titulo;
    private LocalTime horarioProgramado;
    private String caminhoAudioVoz;
    private Cuidador cuidador;
    private Paciente paciente;
    private StatusLembrete status;
    private int quantidadeAdiamentos;
    private LocalDateTime confirmadoEm;

    public Lembrete(int id, String titulo, LocalTime horarioProgramado, String caminhoAudioVoz,
                    Cuidador cuidador, Paciente paciente) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Título do lembrete é obrigatório.");
        }
        this.id = id;
        this.titulo = titulo;
        this.horarioProgramado = horarioProgramado;
        this.caminhoAudioVoz = caminhoAudioVoz;
        this.cuidador = cuidador;
        this.paciente = paciente;
        this.status = StatusLembrete.PENDENTE;
        this.quantidadeAdiamentos = 0;
    }

    public void dispararNotificacaoContextual() {
        System.out.println("\n--- TELA DO PACIENTE (WCAG AAA - Alto Contraste) ---");
        System.out.println("HORÁRIO: " + horarioProgramado + " | TAREFA: " + titulo.toUpperCase());
        if (caminhoAudioVoz != null && !caminhoAudioVoz.isBlank()) {
            System.out.println("Tocando áudio de voz familiar: " + caminhoAudioVoz);
        } else {
            System.out.println("Tocando síntese de voz padrão...");
        }
        System.out.println("[ BOTÃO: JÁ FIZ ]   [ BOTÃO: ADIAR ]");
    }

    public void confirmarRealizacao() {
        this.status = StatusLembrete.CONFIRMADO;
        this.confirmadoEm = LocalDateTime.now();
        System.out.println("[SISTEMA] Lembrete '" + titulo + "' confirmado em: " + confirmadoEm);
        cuidador.receberNotificacaoSucesso("O paciente " + paciente.getNome() + " cumpriu: " + titulo);
    }

    public void adiar(int minutos) {
        if (quantidadeAdiamentos >= 2) {
            this.status = StatusLembrete.ESCALONADO;
            cuidador.receberAlertaVermelho("Paciente atingiu o limite de adiamentos para: " + titulo);
            return;
        }
        this.quantidadeAdiamentos++;
        this.status = StatusLembrete.ADIADO;
        this.horarioProgramado = this.horarioProgramado.plusMinutes(minutos);
        System.out.println("[SISTEMA] Lembrete adiado em " + minutos + " min. Novo horário: " + horarioProgramado);
    }

    public void checarTimeoutEEscalonar(int minutosSemResposta) {
        if (minutosSemResposta < 0) {
            throw new IllegalArgumentException("Minutos sem resposta não pode ser negativo.");
        }
        if (status != StatusLembrete.CONFIRMADO && minutosSemResposta >= 15) {
            this.status = StatusLembrete.ESCALONADO;
            System.out.println("[SISTEMA] Incrementando volume do alarme gradativamente...");
            cuidador.receberAlertaVermelho("O lembrete [" + titulo + "] não foi confirmado há " + minutosSemResposta + " minutos.");
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public LocalTime getHorarioProgramado() { return horarioProgramado; }
    public String getCaminhoAudioVoz() { return caminhoAudioVoz; }
    public Cuidador getCuidador() { return cuidador; }
    public Paciente getPaciente() { return paciente; }
    public StatusLembrete getStatus() { return status; }
    public void setStatus(StatusLembrete status) { this.status = status; }
    public boolean isConfirmado() { return status == StatusLembrete.CONFIRMADO; }
    public LocalDateTime getConfirmadoEm() { return confirmadoEm; }
}
