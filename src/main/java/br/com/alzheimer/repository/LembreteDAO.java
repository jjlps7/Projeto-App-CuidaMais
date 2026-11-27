package br.com.alzheimer.repository;

import br.com.alzheimer.model.*;
import java.sql.*;
import java.time.LocalDateTime;

public class LembreteDAO {
    // Uma coluna por evento garante um único aviso, reaviso e alerta por ocorrência.
    private static final String PROCESSAR = """
        UPDATE lembrete SET
          disparado_em = CASE WHEN status='PENDENTE' THEN COALESCE(disparado_em,?) ELSE disparado_em END,
          redisparado_em = CASE WHEN status='ADIADO' THEN COALESCE(redisparado_em,?) ELSE redisparado_em END,
          entrega = CASE WHEN status='ADIADO' AND reaviso_em IS NULL THEN 'SEM_CONFIRMACAO' ELSE entrega END,
          aviso_em = CASE WHEN status='PENDENTE' THEN COALESCE(aviso_em,agendado) ELSE aviso_em END,
          reaviso_em = CASE WHEN status='ADIADO' THEN COALESCE(reaviso_em,agendado) ELSE reaviso_em END,
          alerta_em = CASE WHEN DATEADD(MINUTE,15,agendado)<=? THEN DATEADD(MINUTE,15,agendado) ELSE alerta_em END
        WHERE paciente_id IN (SELECT id FROM usuario WHERE situacao='ATIVA') AND status<>'CONFIRMADO' AND agendado<=? AND alerta_em IS NULL
          AND ((status='PENDENTE' AND aviso_em IS NULL) OR (status='ADIADO' AND reaviso_em IS NULL)
               OR DATEADD(MINUTE,15,agendado)<=?)
        """;

    public static void processar(Connection c, LocalDateTime agora) throws SQLException {
        atualizarEventos(c,agora,null);
    }

    private static void atualizarEventos(Connection c,LocalDateTime agora,Integer id) throws SQLException {
        try (var p=c.prepareStatement(PROCESSAR + (id==null ? "" : " AND id=?"))) {
            for(int i=1;i<=5;i++) p.setObject(i,agora);
            if(id!=null) p.setInt(6,id);
            p.executeUpdate();
        }
    }

    public void responder(Connection c, int id, int paciente, boolean adiar) throws SQLException {
        responder(c,id,paciente,adiar,LocalDateTime.now());
    }
    public void responder(Connection c,int id,int paciente,boolean adiar,LocalDateTime agora) throws SQLException {
        c.setAutoCommit(false);
        try (var busca = c.prepareStatement("SELECT agendado,status FROM lembrete WHERE id=? AND paciente_id=? FOR UPDATE")) {
            busca.setInt(1,id);
            busca.setInt(2,paciente);
            try (var r = busca.executeQuery()) {
                if (!r.next()) throw new IllegalArgumentException("Lembrete indisponível.");
                var l = new Lembrete(r.getObject(1, LocalDateTime.class), StatusLembrete.valueOf(r.getString(2)));
                atualizarEventos(c,agora,id);
                l.responder(adiar, agora);
                try (var p = c.prepareStatement("UPDATE lembrete SET agendado=?, status=?, confirmado_em=? WHERE id=?")) {
                    p.setObject(1,l.getAgendado());
                    p.setString(2,l.getStatus().name());
                    p.setObject(3,adiar ? null : agora);
                    p.setInt(4,id);
                    p.executeUpdate();
                }
            }
            c.commit();
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally { c.setAutoCommit(true); }
    }
}
