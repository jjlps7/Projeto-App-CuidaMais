package br.com.alzheimer.model;

public final class Cuidador extends Usuario {
    public Cuidador(int id, String nome, String email, String hash) {
        super(id, nome, email, hash);
    }
    @Override public String getPerfil() { return "CUIDADOR"; }
    @Override public boolean gerenciaRotina() { return true; }
}
