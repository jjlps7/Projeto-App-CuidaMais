package br.com.alzheimer.repository;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import org.h2.tools.RunScript;

public class ConexaoBD {
    private static final String URL = "jdbc:h2:file:" + System.getProperty("cuida.banco", "./dados/rotina");
    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, "rotina_app", "rotina-demo");
    }
    public static void inicializarBanco() throws Exception {
        try (var conn = DriverManager.getConnection(URL, "sa", "");
             var script = new InputStreamReader(ConexaoBD.class.getResourceAsStream("/schema.sql"), StandardCharsets.UTF_8)) {
            RunScript.execute(conn, script);
            // A primeira versão do H2 gerava nomes automáticos para os dois CHECK de perfil.
            var antigos = new java.util.ArrayList<String>();
            try (var st = conn.createStatement();
                 var rs = st.executeQuery("""
                     SELECT t.constraint_name FROM information_schema.table_constraints t
                     JOIN information_schema.check_constraints k
                       ON k.constraint_schema=t.constraint_schema AND k.constraint_name=t.constraint_name
                     WHERE t.table_schema='PUBLIC' AND t.table_name='USUARIO'
                       AND t.constraint_name LIKE 'CONSTRAINT_%'
                       AND k.check_clause LIKE '%"PERFIL"%'
                       AND k.check_clause LIKE '%CUIDADOR%' AND k.check_clause LIKE '%PACIENTE%'
                       AND k.check_clause NOT LIKE '%FAMILIAR%'
                     """)) {
                while (rs.next()) antigos.add(rs.getString(1));
            }
            try (var st = conn.createStatement()) {
                for (String nome : antigos)
                    st.execute("ALTER TABLE usuario DROP CONSTRAINT " + st.enquoteIdentifier(nome,true));
            }
        }
    }
}
