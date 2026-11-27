import br.com.alzheimer.repository.LembreteDAO;
import java.sql.*;
import java.time.LocalDateTime;
import java.nio.file.*;
import org.h2.tools.RunScript;

class TestePrazos {
    static LocalDateTime hora(String minuto) { return LocalDateTime.parse("2026-11-27T"+minuto); }
    static void conferir(Connection c,int id,String esperado) throws Exception {
        try(var p=c.prepareStatement("select estado_atual,agendado,aviso_em,reaviso_em,alerta_em,confirmado_em from agenda_rotina where id=?")) {
            p.setInt(1,id);
            try(var r=p.executeQuery()) {
                r.next(); String resultado=r.getString(1);
                for(int i=2;i<=6;i++){var t=r.getObject(i,LocalDateTime.class);resultado+=" "+(t==null?"-":t.toLocalTime().toString());}
                if(!resultado.equals(esperado)) throw new AssertionError(resultado+" != "+esperado);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        try(var admin=DriverManager.getConnection("jdbc:h2:mem:prazos","sa","")) {
            RunScript.execute(admin,Files.newBufferedReader(Path.of("database/schema.sql")));
            try(var c=DriverManager.getConnection("jdbc:h2:mem:prazos","rotina_app","rotina-demo");var st=c.createStatement()) {
                st.execute("insert into usuario(nome,email,senha_hash,perfil) values('C','c','x','CUIDADOR')");
                st.execute("insert into usuario(nome,email,senha_hash,perfil,cuidador_id) values('P','p','x','PACIENTE',1)");
                for(int i=0;i<4;i++) st.execute("insert into lembrete(paciente_id,titulo,categoria,agendado) values(2,'Teste','Higiene','2026-11-27 08:00')");
                var dao=new LembreteDAO();
                LembreteDAO.processar(c,hora("07:59"));
                conferir(c,1,"PENDENTE 08:00 - - - -");
                LembreteDAO.processar(c,hora("08:00"));
                conferir(c,1,"PENDENTE 08:00 08:00 - - -");
                dao.responder(c,1,2,true,hora("08:05"));
                dao.responder(c,2,2,true,hora("08:05"));
                dao.responder(c,2,2,false,hora("08:12"));
                dao.responder(c,3,2,false,hora("08:14"));
                LembreteDAO.processar(c,hora("08:19"));
                conferir(c,1,"ADIADO 08:20 08:00 - - -");
                LembreteDAO.processar(c,hora("08:20"));
                conferir(c,1,"PENDENTE 08:20 08:00 08:20 - -");
                try {dao.responder(c,1,2,true,hora("08:21"));throw new AssertionError();}catch(IllegalArgumentException ok){}
                LembreteDAO.processar(c,hora("08:34"));
                conferir(c,1,"PENDENTE 08:20 08:00 08:20 - -");
                LembreteDAO.processar(c,hora("08:35"));
                LembreteDAO.processar(c,hora("08:36"));
                conferir(c,1,"ALERTA 08:20 08:00 08:20 08:35 -");
                dao.responder(c,1,2,false,hora("08:37"));
                conferir(c,1,"CONFIRMADO 08:20 08:00 08:20 08:35 08:37");
                conferir(c,2,"CONFIRMADO 08:20 08:00 - - 08:12");
                conferir(c,3,"CONFIRMADO 08:00 08:00 - - 08:14");
                conferir(c,4,"ALERTA 08:00 08:00 - 08:15 -");
                System.out.println("OK: disparo, adiamento unico, reaviso, alerta e confirmacoes antes/depois do prazo.");
            }
        }
    }
}
