package br.com.alzheimer.model;

import java.time.LocalDateTime;

public class Lembrete {
    private LocalDateTime agendado;
    private StatusLembrete status;
    public Lembrete(LocalDateTime agendado, StatusLembrete status) {
        this.agendado = agendado;
        this.status = status;
    }
    public void responder(boolean adiar, LocalDateTime agora) {
        if (agendado.isAfter(agora) && status != StatusLembrete.ADIADO) throw new IllegalArgumentException("Aguarde o horário do lembrete.");
        if (status == StatusLembrete.CONFIRMADO) throw new IllegalArgumentException("Lembrete já confirmado.");
        if (adiar && status != StatusLembrete.PENDENTE) throw new IllegalArgumentException("Este lembrete já foi adiado.");
        if (adiar && !agora.isBefore(agendado.plusMinutes(15))) throw new IllegalArgumentException("O prazo para adiar terminou.");
        status = adiar ? StatusLembrete.ADIADO : StatusLembrete.CONFIRMADO;
        if (adiar) agendado = agora.plusMinutes(15);
    }
    public LocalDateTime getAgendado() { return agendado; }
    public StatusLembrete getStatus() { return status; }
}
