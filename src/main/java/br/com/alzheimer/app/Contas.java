package br.com.alzheimer.app;

import br.com.alzheimer.model.Senhas;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;

final class Contas {
    record Acao(int usuario,String tipo,String email) {}
    static String email(String valor) {
        String email=valor.strip().toLowerCase(Locale.ROOT);
        if(email.length()>160 || !email.matches("[^@\\s\\p{Cntrl}]+@[^@\\s\\p{Cntrl}]+\\.[^@\\s\\p{Cntrl}]+"))
            throw new IllegalArgumentException("Informe um e-mail válido.");
        return email;
    }
    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
    static int executar(Connection c,String sql,Object... valores) throws SQLException {
        try(var p=c.prepareStatement(sql)) {
            for(int i=0;i<valores.length;i++) p.setObject(i+1,valores[i]);
            return p.executeUpdate();
        }
    }
    static void emitir(Connection c,int id,String tipo,String destino) throws SQLException {
        byte[] bytes=new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        executar(c,"DELETE FROM acao_conta WHERE usuario_id=? AND tipo=?",id,tipo);
        executar(c,"INSERT INTO acao_conta(token_hash,usuario_id,tipo,email,expira) VALUES(?,?,?,?,DATEADD(MINUTE,30,CURRENT_TIMESTAMP))",
            hash(token),id,tipo,destino);
        String assunto=switch(tipo) {
            case "SENHA" -> "CuidaMais · Redefinir senha";
            case "EMAIL" -> "CuidaMais · Confirmar novo e-mail";
            default -> "CuidaMais · Confirmar cadastro";
        };
        Correio.enviar(destino,assunto,token);
    }
    static void solicitar(Connection c,String email) throws SQLException {
        try(var p=c.prepareStatement("SELECT id,situacao FROM usuario WHERE email=? AND situacao<>'ENCERRADA' FOR UPDATE")) {
            p.setString(1,email.strip().toLowerCase(Locale.ROOT));
            try(var r=p.executeQuery()){
                if(r.next()) emitir(c,r.getInt(1),r.getString(2).equals("PENDENTE")?"ATIVAR":"SENHA",email.strip().toLowerCase(Locale.ROOT));
            }
        }
    }
    static Acao buscar(Connection c,String token,boolean bloquear) throws SQLException {
        try(var p=c.prepareStatement("SELECT usuario_id,tipo,email FROM acao_conta WHERE token_hash=? AND expira>CURRENT_TIMESTAMP"+(bloquear?" FOR UPDATE":""))) {
            p.setString(1,hash(token));
            try(var r=p.executeQuery()){
                if(r.next()) return new Acao(r.getInt(1),r.getString(2),r.getString(3));
            }
        }
        throw new IllegalArgumentException("Link expirado ou já utilizado. Solicite outro link.");
    }
    static int concluir(Connection c,String token,String senha) throws SQLException {
        var a=buscar(c,token,true);
        switch(a.tipo()) {
            case "ATIVAR" -> executar(c,"UPDATE usuario SET situacao='ATIVA',senha_hash=? WHERE id=? AND situacao='PENDENTE'",Senhas.criar(senha),a.usuario());
            case "SENHA" -> {
                try(var p=c.prepareStatement("SELECT senha_hash FROM usuario WHERE id=? FOR UPDATE")) {
                    p.setInt(1,a.usuario());
                    try(var r=p.executeQuery()) {
                        if(r.next() && Senhas.conferir(senha,r.getString(1)))
                            throw new IllegalArgumentException("Escolha uma senha diferente da anterior.");
                    }
                }
                executar(c,"UPDATE usuario SET senha_hash=? WHERE id=? AND situacao='ATIVA'",Senhas.criar(senha),a.usuario());
            }
            case "EMAIL" -> executar(c,"UPDATE usuario SET email=? WHERE id=? AND situacao='ATIVA'",a.email(),a.usuario());
            default -> throw new IllegalArgumentException("Operação inválida.");
        }
        executar(c,"DELETE FROM acao_conta WHERE usuario_id=?",a.usuario());
        return a.usuario();
    }
    static void encerrar(Connection c,int id) throws SQLException {
        try(var p=c.prepareStatement("SELECT 1 FROM usuario WHERE cuidador_id=? AND situacao='ATIVA'")) {
            p.setInt(1,id);
            try(var r=p.executeQuery()){
                if(r.next()) throw new IllegalArgumentException("Você ainda é responsável por um paciente. Peça ao paciente que transfira a responsabilidade em Minha conta ou encerre o próprio perfil antes de continuar.");
            }
        }
        executar(c,"UPDATE usuario SET cuidador_id=NULL WHERE cuidador_id=? AND situacao='PENDENTE'",id);
        executar(c,"DELETE FROM vinculo_familiar WHERE familiar_id=? OR paciente_id=?",id,id);
        executar(c,"DELETE FROM acao_conta WHERE usuario_id=?",id);
        executar(c,"UPDATE usuario SET situacao='ENCERRADA',cuidador_id=NULL,senha_hash='',email=? WHERE id=?","encerrada-"+UUID.randomUUID(),id);
    }
}
