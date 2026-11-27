package br.com.alzheimer.model;

public final class Familiar extends Usuario {
    public Familiar(int id, String nome, String email, String hash) {
        super(id, nome, email, hash);
    }
    @Override public String getPerfil() { return "FAMILIAR"; }
    @Override public boolean gerenciaRotina() { return false; }
}
