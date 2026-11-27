package br.com.alzheimer.app;

import java.sql.*;
import java.time.*;
import java.util.*;
import br.com.alzheimer.model.Usuario;
import br.com.alzheimer.repository.LembreteDAO;

final class Programacoes {
    private static int inserir(Connection c,String sql,Object... valores) throws SQLException {
        try(var p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            for(int i=0;i<valores.length;i++) p.setObject(i+1,valores[i]);
            p.executeUpdate();
            try(var r=p.getGeneratedKeys()){r.next();return r.getInt(1);}
        }
    }

    static void salvar(Connection c,Usuario cuidador,int paciente,Map<String,String> f,String rota) throws SQLException {
        boolean criar=rota.equals("/agendar");
        boolean desativar=rota.equals("/desativar");
        c.setAutoCommit(false);
        int id=criar ? inserir(c,"INSERT INTO programacao(paciente_id) VALUES(?)",paciente) : Integer.parseInt(Main.texto(f,"programa",12));
        int revisao=1;
        Integer voz=null;
        LocalDateTime agora=LocalDateTime.now();
        if(!criar) {
            try(var p=c.prepareStatement("SELECT revisao FROM programacao WHERE id=? AND paciente_id=? AND ativa FOR UPDATE")) {
                p.setInt(1,id);p.setInt(2,paciente);
                try(var r=p.executeQuery()){
                    if(!r.next()) throw new IllegalArgumentException("Programação indisponível.");
                    revisao=r.getInt(1)+1;
                }
            }
            if(Integer.parseInt(Main.texto(f,"revisao",10))!=revisao-1)
                throw new IllegalArgumentException("Esta programação foi alterada. Atualize a página antes de editar.");
            gerar(c,id,agora);
            LembreteDAO.processar(c,agora);
            Contas.executar(c,"DELETE FROM lembrete WHERE programa=? AND agendado>? AND aviso_em IS NULL AND status='PENDENTE'",id,agora);
            if(desativar) {
                Contas.executar(c,"INSERT INTO versao_rotina(programa,revisao,autor,acao,titulo,categoria,inicio,repeticao,dias,instrucao,voz_id) SELECT programa,?,?, 'DESATIVADA',titulo,categoria,inicio,repeticao,dias,instrucao,voz_id FROM versao_rotina WHERE programa=? AND revisao=?",revisao,cuidador.getId(),id,revisao-1);
                Contas.executar(c,"UPDATE programacao SET ativa=FALSE,revisao=?,proxima=NULL WHERE id=?",revisao,id);
                c.commit();return;
            }
            try(var p=c.prepareStatement("SELECT voz_id FROM versao_rotina WHERE programa=? AND revisao=?")){
                p.setInt(1,id);p.setInt(2,revisao-1);
                try(var r=p.executeQuery()){r.next();voz=r.getObject(1,Integer.class);}
            }
        }
        var inicio=LocalDateTime.parse(Main.texto(f,"horario",19));
        String tipo=Main.texto(f,"repeticao",8);
        if(!Set.of("UNICA","DIARIA","SEMANAL").contains(tipo)) throw new IllegalArgumentException("Escolha a repetição.");
        String dias="";
        if(tipo.equals("SEMANAL")){
            for(int i=1;i<=7;i++) if(f.containsKey("dia"+i)) dias+=i;
            if(dias.isEmpty()) throw new IllegalArgumentException("Selecione pelo menos um dia da semana.");
        }
        String titulo=Main.texto(f,"titulo",150),categoria=Main.texto(f,"categoria",20);
        if(!Set.of("Medicamento","Alimentação","Higiene","Atividade").contains(categoria)) throw new IllegalArgumentException("Escolha a categoria.");
        String instrucao=f.getOrDefault("instrucao","").strip();
        if(instrucao.length()>500) throw new IllegalArgumentException("Use até 500 caracteres na instrução.");
        if(f.containsKey("retirar")) voz=null;
        if(!f.getOrDefault("gravacao","").isEmpty()) {
            byte[] mp3=Base64.getDecoder().decode(f.get("gravacao"));
            if(!mp3Valido(mp3)) throw new IllegalArgumentException("Selecione um arquivo MP3 de até 2 MB.");
            voz=inserir(c,"INSERT INTO voz(mp3) VALUES(?)",mp3);
        }
        Contas.executar(c,"INSERT INTO versao_rotina(programa,revisao,autor,acao,titulo,categoria,inicio,repeticao,dias,instrucao,voz_id) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            id,revisao,cuidador.getId(),criar?"CRIADA":"EDITADA",titulo,categoria,inicio,tipo,dias,instrucao,voz);
        LocalDateTime primeira=inicio;
        if(!tipo.equals("UNICA") && primeira.toLocalDate().isBefore(agora.toLocalDate()))
            primeira=agora.toLocalDate().atTime(inicio.toLocalTime());
        if(!criar && !primeira.isAfter(agora)) primeira=tipo.equals("UNICA")?null:primeira.plusDays(1);
        primeira=proximoDia(primeira,tipo,dias);
        Contas.executar(c,"UPDATE programacao SET revisao=?,proxima=? WHERE id=?",revisao,primeira,id);
        gerar(c,id,agora);
        c.commit();
    }
    private static LocalDateTime proximoDia(LocalDateTime data,String tipo,String dias) {
        if(data!=null && tipo.equals("SEMANAL"))
            while(!dias.contains(String.valueOf(data.getDayOfWeek().getValue()))) data=data.plusDays(1);
        return data;
    }
    static void avancar(Connection c,LocalDateTime agora) throws SQLException {
        var ids=new ArrayList<Integer>();
        try(var p=c.prepareStatement("SELECT p.id FROM programacao p JOIN usuario u ON u.id=p.paciente_id WHERE p.ativa AND u.situacao='ATIVA' AND p.proxima<=?")) {
            p.setObject(1,agora.plusDays(7));
            try(var r=p.executeQuery()){while(r.next()) ids.add(r.getInt(1));}
        }
        for(int id:ids) {
            c.setAutoCommit(false);
            try {gerar(c,id,agora);c.commit();}
            catch(SQLException | RuntimeException e){c.rollback();throw e;}
            finally {c.setAutoCommit(true);}
        }
    }
    private static void gerar(Connection c,int id,LocalDateTime agora) throws SQLException {
        try(var p=c.prepareStatement("SELECT proxima,revisao,paciente_id FROM programacao WHERE id=? AND ativa FOR UPDATE")){
            p.setInt(1,id);
            try(var r=p.executeQuery()){
                if(!r.next()) return;
                var data=r.getObject(1,LocalDateTime.class);
                if(data==null) return;
                int rev=r.getInt(2),paciente=r.getInt(3);
                try(var v=c.prepareStatement("SELECT * FROM versao_rotina WHERE programa=? AND revisao=?")){
                    v.setInt(1,id);v.setInt(2,rev);
                    try(var s=v.executeQuery()){
                        s.next();
                        String tipo=s.getString("repeticao"),dias=s.getString("dias");
                        // Inclui a primeira data mesmo que o início seja daqui a mais de uma semana.
                        do {
                            var dia=data.toLocalDate().atStartOfDay();
                            Contas.executar(c,"INSERT INTO lembrete(programa,revisao,previsto,paciente_id,titulo,categoria,agendado,instrucao,autor_id) SELECT ?,?,?,?,?,?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM lembrete WHERE programa=? AND previsto>=? AND previsto<?)",
                                id,rev,data,paciente,s.getString("titulo"),s.getString("categoria"),data,s.getString("instrucao"),s.getObject("autor"),id,dia,dia.plusDays(1));
                            data=tipo.equals("UNICA")?null:proximoDia(data.plusDays(1),tipo,dias);
                        } while(data!=null && !data.isAfter(agora.plusDays(7)));
                        Contas.executar(c,"UPDATE programacao SET proxima=? WHERE id=?",data,id);
                    }
                }
            }
        }
    }
    private static boolean mp3Valido(byte[] b) {
        if(b.length<4 || b.length>2097152) return false;
        int i=0;
        if(b.length>=10 && b[0]=='I' && b[1]=='D' && b[2]=='3') {
            int tag=0;
            for(int j=6;j<10;j++){if(b[j]<0)return false;tag=(tag<<7)|b[j];}
            i=10+tag;
        }
        return i+4<=b.length && (b[i]&255)==255 && (b[i+1]&0xe6)==0xe2
            && (b[i+1]&0x18)!=8 && (b[i+2]&0xf0)!=0xf0 && (b[i+2]&0x0c)!=0x0c;
    }
}
