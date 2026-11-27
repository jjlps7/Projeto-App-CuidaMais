package br.com.alzheimer.model;

public class Paciente extends Usuario {
    private int cuidadorId;
    private String nivelAlzheimer;

    public Paciente(int id, String nome, String email, String senhaHash, int cuidadorId, String nivelAlzheimer) {
        super(id, nome, email, senhaHash);
        this.cuidadorId = cuidadorId;
        this.nivelAlzheimer = nivelAlzheimer;
    }

    @Override
    public String getPerfil() {
        return "PACIENTE";
    }

    @Override
    public void receberNotificacao(String nivel, String mensagem) {
        System.out.println("[TELA ALTO CONTRASTE - " + getNome().toUpperCase() + "] " + mensagem);
    }

    public int getCuidadorId() { return cuidadorId; }
    public String getNivelAlzheimer() { return nivelAlzheimer; }
}
