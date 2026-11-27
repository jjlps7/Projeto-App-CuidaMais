package br.com.alzheimer.model;

public abstract class Usuario {
    private final int id;
    private final String nome;
    private final String email;
    private final String senhaHash;

    protected Usuario(int id, String nome, String email, String senhaHash) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome obrigatório.");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("E-mail inválido.");
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
    }
    public abstract String getPerfil();
    public abstract boolean gerenciaRotina();
    public boolean autenticar(String senha) { return Senhas.conferir(senha, senhaHash); }
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
}
