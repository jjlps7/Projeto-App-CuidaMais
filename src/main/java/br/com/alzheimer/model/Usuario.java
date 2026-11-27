package br.com.alzheimer.model;

public abstract class Usuario implements Notificavel {
    private int id;
    private String nome;
    private String email;
    private String senhaHash;

    public Usuario(int id, String nome, String email, String senhaHash) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome não pode ser vazio.");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
    }

    public abstract String getPerfil();

    public boolean autenticar(String senha) {
        return this.senhaHash != null && this.senhaHash.equals(senha);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
}
