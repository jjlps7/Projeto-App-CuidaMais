package br.com.alzheimer.model;

public class Cuidador extends Usuario {
    private String telefoneEmergencia;

    public Cuidador(int id, String nome, String email, String senhaHash, String telefoneEmergencia) {
        super(id, nome, email, senhaHash);
        this.telefoneEmergencia = telefoneEmergencia;
    }

    @Override
    public String getPerfil() {
        return "CUIDADOR";
    }

    @Override
    public void receberNotificacao(String nivel, String mensagem) {
        System.out.println("[PUSH CUIDADOR - " + nivel + "] (" + getNome() + "): " + mensagem);
    }

    public void receberAlertaVermelho(String mensagem) {
        receberNotificacao("ALERTA VERMELHO", mensagem);
    }

    public void receberNotificacaoSucesso(String mensagem) {
        receberNotificacao("SUCESSO", mensagem);
    }

    public String getTelefoneEmergencia() { return telefoneEmergencia; }
}
