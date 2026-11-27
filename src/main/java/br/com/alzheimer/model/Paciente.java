package br.com.alzheimer.model;

public final class Paciente extends Usuario {
    public Paciente(int id, String nome, String email, String hash) {
        super(id, nome, email, hash);
    }
    @Override public String getPerfil() { return "PACIENTE"; }
    @Override public boolean gerenciaRotina() { return false; }
}
