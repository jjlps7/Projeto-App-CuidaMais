package br.com.alzheimer.model;

import java.time.LocalDateTime;

public class RegistroComportamental {
    private int id;
    private int pacienteId;
    private String categoria;
    private int intensidade;
    private String observacao;
    private LocalDateTime registradoEm;

    public RegistroComportamental(int id, int pacienteId, String categoria, int intensidade, String observacao) {
        if (intensidade < 1 || intensidade > 5) {
            throw new IllegalArgumentException("Intensidade deve estar entre 1 e 5.");
        }
        this.id = id;
        this.pacienteId = pacienteId;
        this.categoria = categoria;
        this.intensidade = intensidade;
        this.observacao = observacao;
        this.registradoEm = LocalDateTime.now();
    }

    public boolean isCritico() {
        return intensidade >= 4;
    }

    public int getId() { return id; }
    public int getPacienteId() { return pacienteId; }
    public String getCategoria() { return categoria; }
    public int getIntensidade() { return intensidade; }
    public String getObservacao() { return observacao; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
}
