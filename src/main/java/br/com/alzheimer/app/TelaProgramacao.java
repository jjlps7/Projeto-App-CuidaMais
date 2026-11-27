package br.com.alzheimer.app;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

final class TelaProgramacao {
    static String campos(ResultSet v) throws SQLException {
        StringBuilder html=new StringBuilder();
        html.append("<label>Título<input required maxlength='150' name='titulo' value=\"").append(v==null?"":Tela.esc(v.getString("titulo"))).append("\"></label>");
        html.append("<label>Categoria<select name='categoria'>");
        for(String item:new String[]{"Alimentação","Medicamento","Higiene","Atividade"})
            html.append(option(item,item,v==null?"":v.getString("categoria")));
        html.append("</select></label><label>Início e horário<input required type='datetime-local' name='horario' value='")
            .append(v==null?"":v.getObject("inicio",LocalDateTime.class).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))).append("'></label>");
        String tipo=v==null?"UNICA":v.getString("repeticao");
        html.append("<label>Repetição<select name='repeticao'>").append(option("UNICA","Uma vez",tipo))
            .append(option("DIARIA","Todos os dias",tipo)).append(option("SEMANAL","Dias da semana",tipo)).append("</select></label>");
        html.append("<fieldset class='dias'").append(tipo.equals("SEMANAL")?"":" hidden").append("><legend>Dias da semana</legend>");
        String[] nomes={"Seg","Ter","Qua","Qui","Sex","Sáb","Dom"};
        for(int i=1;i<=7;i++) html.append("<label><input type='checkbox' name='dia").append(i).append("'")
            .append(v!=null && v.getString("dias").contains(""+i)?" checked":"").append(">").append(nomes[i-1]).append("</label>");
        html.append("</fieldset><label>Instrução (opcional)<input maxlength='500' name='instrucao' value=\"")
            .append(v==null?"":Tela.esc(v.getString("instrucao"))).append("\"></label>")
            .append("<label>Voz familiar (MP3, até 2 MB)<input type='file' name='arquivo' accept='.mp3,audio/mpeg'></label>");
        if(v!=null && v.getObject("voz_id")!=null) html.append("<label><input type='checkbox' name='retirar'>Retirar gravação atual</label>");
        return html.toString();
    }
    private static String option(String valor,String legenda,String atual) {
        return "<option value='"+valor+"'"+(valor.equals(atual)?" selected":"")+">"+legenda+"</option>";
    }
    static String gerenciar(Connection c,int cuidador) throws SQLException {
        StringBuilder b=new StringBuilder("<details><summary>Gerenciar programações</summary>");
        try(var p=c.prepareStatement("""
            SELECT v.*,p.paciente_id,p.ativa,u.nome FROM versao_rotina v JOIN programacao p
            ON p.id=v.programa AND p.revisao=v.revisao JOIN usuario u ON u.id=p.paciente_id
            WHERE u.cuidador_id=? AND u.situacao='ATIVA' ORDER BY p.ativa DESC,v.titulo
            """)) {
            p.setInt(1,cuidador);
            try(var r=p.executeQuery()){
                if(!r.isBeforeFirst()) b.append("<p>Nenhuma programação cadastrada.</p>");
                while(r.next()) {
                    b.append("<details><summary>").append(Tela.esc(r.getString("titulo"))).append(" · ").append(Tela.esc(r.getString("nome"))).append(r.getBoolean("ativa")?"":" · Desativada").append("</summary>");
                    String ids="<input type='hidden' name='programa' value='"+r.getInt("programa")+"'><input type='hidden' name='revisao' value='"+r.getInt("revisao")+"'><input type='hidden' name='paciente' value='"+r.getInt("paciente_id")+"'>";
                    if(r.getBoolean("ativa")) b.append(Tela.form("editar",ids+campos(r),"Salvar alteração")).append(Tela.form("desativar",ids,"Desativar lembrete"));
                    b.append("</details>");
                }
            }
        }
        b.append("<details><summary>Histórico de alterações</summary>");
        try(var p=c.prepareStatement("""
            SELECT v.*,a.nome AS responsavel FROM versao_rotina v JOIN programacao p ON p.id=v.programa
            JOIN usuario pac ON pac.id=p.paciente_id LEFT JOIN usuario a ON a.id=v.autor
            WHERE pac.cuidador_id=? AND pac.situacao='ATIVA' ORDER BY v.alterada_em DESC,v.revisao DESC
            """)) {
            p.setInt(1,cuidador);
            try(var r=p.executeQuery()){
                while(r.next()) b.append("<p>").append(Tela.esc(r.getString("titulo"))).append(" · ").append(r.getString("acao"))
                    .append(" · versão ").append(r.getInt("revisao")).append(" · ").append(r.getTimestamp("alterada_em"))
                    .append(" · ").append(Tela.esc(r.getString("responsavel"))).append(" · início ").append(r.getTimestamp("inicio"))
                    .append(" · ").append(r.getString("repeticao")).append("</p>");
            }
        }
        return b.append("</details></details>").toString();
    }
}
