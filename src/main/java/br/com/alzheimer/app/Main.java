package br.com.alzheimer.app;

import br.com.alzheimer.model.*;
import br.com.alzheimer.repository.*;
import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private record Sessao(Usuario usuario, Instant vence) {}
    private static final Map<String,Sessao> sessoes = new ConcurrentHashMap<>();
    public static void main(String[] args) throws Exception {
        ConexaoBD.inicializarBanco();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", Integer.getInteger("cuida.porta",8080)),0);
        server.createContext("/", Main::atender);
        var relogio = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        relogio.scheduleWithFixedDelay(() -> {
            try (var c=ConexaoBD.conectar()) {
                var agora=LocalDateTime.now();
                Programacoes.avancar(c,agora);
                LembreteDAO.processar(c,agora);
            } catch (SQLException e) { e.printStackTrace(); }
        },0,2,java.util.concurrent.TimeUnit.SECONDS);
        server.start();
        System.out.println("CuidaMais em http://localhost:" + server.getAddress().getPort());
    }
    private static Map<String,String> campos(String bruto) {
        var dados = new HashMap<String,String>();
        for (String par : bruto.split("&")) {
            String[] kv = par.split("=",2);
            if (kv.length==2) dados.put(kv[0], URLDecoder.decode(kv[1],StandardCharsets.UTF_8));
        }
        return dados;
    }
    static String texto(Map<String,String> f,String campo,int limite) {
        var v=f.getOrDefault(campo,"").strip();
        if(v.isEmpty() || v.length()>limite) throw new IllegalArgumentException("Confira o campo " + campo + ".");
        return v;
    }
    private static void atender(HttpExchange x) throws java.io.IOException {
        try (var c=ConexaoBD.conectar()) {
            String rota=x.getRequestURI().getPath();
            String token=Arrays.stream(Optional.ofNullable(x.getRequestHeaders().getFirst("Cookie")).orElse("").split(";"))
                .map(String::strip).filter(s->s.startsWith("cuida=")).map(s->s.substring(6)).findFirst().orElse("");
            Sessao sessao=sessoes.get(token);
            Usuario u=sessao!=null && sessao.vence().isAfter(Instant.now()) ? sessao.usuario() : null;
            if (x.getRequestMethod().equals("GET") && rota.equals("/voz")) {
                if(u==null) { pagina(x,401,"Entre para continuar.");return; }
                var query=campos(Optional.ofNullable(x.getRequestURI().getRawQuery()).orElse(""));
                int id=Integer.parseInt(texto(query,"id",12));
                try(var ps=c.prepareStatement("""
                    SELECT a.mp3 FROM lembrete l JOIN versao_rotina v ON v.programa=l.programa AND v.revisao=l.revisao
                    JOIN voz a ON a.id=v.voz_id JOIN usuario pac ON pac.id=l.paciente_id
                    WHERE l.id=? AND pac.situacao='ATIVA' AND (pac.id=? OR pac.cuidador_id=?)
                    """)){
                    ps.setInt(1,id);ps.setInt(2,u.getId());ps.setInt(3,u.getId());
                    try(var r=ps.executeQuery()){
                        if(!r.next()){pagina(x,404,"Áudio indisponível.");return;}
                        byte[] mp3=r.getBytes(1);
                        x.getResponseHeaders().set("Content-Type","audio/mpeg");
                        x.getResponseHeaders().set("Cache-Control","private, no-store");
                        x.sendResponseHeaders(200,mp3.length);x.getResponseBody().write(mp3);return;
                    }
                }
            }
            if (x.getRequestMethod().equals("GET") && rota.equals("/validar")) {
                var query=campos(Optional.ofNullable(x.getRequestURI().getRawQuery()).orElse(""));
                String chave=query.getOrDefault("token","");
                var acao=Contas.buscar(c,chave,false);
                pagina(x,200,Tela.validar(c,acao,chave)); return;
            }
            if (x.getRequestMethod().equals("GET") && rota.equals("/")) {
                var filtro=campos(Optional.ofNullable(x.getRequestURI().getRawQuery()).orElse(""));
                pagina(x,200,Tela.avisoConta(filtro.getOrDefault("ok",""))+(u==null ? Tela.entrada() : Tela.painel(c,u,filtro)));
                return;
            }
            if (!x.getRequestMethod().equals("POST")) { pagina(x,404,"Página indisponível."); return; }
            String origem=x.getRequestHeaders().getFirst("Origin");
            if(origem!=null && !origem.equals("http://"+x.getRequestHeaders().getFirst("Host"))) {
                pagina(x,403,"Origem inválida."); return;
            }
            int limite=u instanceof Cuidador && Set.of("/agendar","/editar").contains(rota)?9*1024*1024:16384;
            byte[] corpo=x.getRequestBody().readNBytes(limite+1);
            if(corpo.length>limite) { pagina(x,413,"Formulário muito grande."); return; }
            var f=campos(new String(corpo,StandardCharsets.UTF_8));
            String destino="/";
            if(Set.of("/cadastro","/paciente","/recuperar","/validar","/novo-email","/encerrar","/nome","/transferir").contains(rota))
                c.setAutoCommit(false);
            if(rota.equals("/cadastro") && u==null) {
                cadastrar(c,f,null); destino="/?ok=cadastro";
            } else if(rota.equals("/recuperar")) {
                try { Contas.solicitar(c,f.getOrDefault("email","")); }
                catch(IllegalStateException e) { c.rollback(); System.err.println("Falha de entrega na recuperação de conta."); }
                destino="/?ok=recuperar";
            } else if(rota.equals("/validar")) {
                int dono=Contas.concluir(c,f.getOrDefault("token",""),f.getOrDefault("senha",""));
                c.commit(); invalidar(dono); destino="/?ok=validado";
            }
            else if(rota.equals("/entrar") && u==null) {
                var conta=login(c,f);
                sessoes.values().removeIf(s->s.vence().isBefore(Instant.now()));
                String novo=UUID.randomUUID().toString();
                sessoes.put(novo,new Sessao(conta,Instant.now().plusSeconds(14400)));
                x.getResponseHeaders().add("Set-Cookie","cuida="+novo+"; Path=/; HttpOnly; SameSite=Strict; Max-Age=14400");
            } else if(u==null) { pagina(x,401,"Entre para continuar."); return; }
            else if(rota.equals("/sair")) {
                sessoes.remove(token);
                x.getResponseHeaders().add("Set-Cookie","cuida=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
            } else if(u.gerenciaRotina() && rota.equals("/paciente")) {
                cadastrar(c,f,u); destino="/?ok=cadastro";
            } else if(rota.equals("/nome") && !(u instanceof Paciente)) {
                Contas.executar(c,"UPDATE usuario SET nome=? WHERE id=?",texto(f,"nome",120),u.getId());
                c.commit(); invalidar(u.getId()); destino="/?ok=nome";
            } else if(rota.equals("/novo-email") || rota.equals("/encerrar") || rota.equals("/transferir") && u instanceof Paciente) {
                if(!u.autenticar(f.getOrDefault("senha",""))) throw new IllegalArgumentException("Senha atual incorreta.");
                if(rota.equals("/novo-email")) {
                    String novo=Contas.email(f.getOrDefault("email",""));
                    try(var ps=c.prepareStatement("SELECT 1 FROM usuario WHERE email=?")) {
                        ps.setString(1,novo);
                        try(var r=ps.executeQuery()) { if(r.next()) throw new IllegalArgumentException("Escolha outro e-mail."); }
                    }
                    Contas.emitir(c,u.getId(),"EMAIL",novo); destino="/?ok=email";
                } else if(rota.equals("/transferir")) {
                    String novo=Contas.email(f.getOrDefault("email",""));
                    int linhas=Contas.executar(c,"UPDATE usuario SET cuidador_id=(SELECT id FROM usuario WHERE email=?) WHERE id=? AND EXISTS(SELECT 1 FROM usuario WHERE email=? AND perfil='CUIDADOR' AND situacao='ATIVA')",novo,u.getId(),novo);
                    if(linhas==0) throw new IllegalArgumentException("Informe um cuidador com conta ativa.");
                    destino="/?ok=transferido";
                } else {
                    if(!"sim".equals(f.get("confirmar"))) throw new IllegalArgumentException("Confirme o encerramento da conta.");
                    Contas.encerrar(c,u.getId()); c.commit(); invalidar(u.getId()); destino="/?ok=encerrada";
                }
            }
            else if(u instanceof Paciente && rota.equals("/autorizar")) {
                try (var ps = c.prepareStatement("""
                    INSERT INTO vinculo_familiar(paciente_id,familiar_id)
                    SELECT ?, id FROM usuario WHERE perfil='FAMILIAR' AND situacao='ATIVA' AND email=?
                    """)) {
                    ps.setInt(1,u.getId());
                    ps.setString(2,texto(f,"email",160).toLowerCase(Locale.ROOT));
                    if(ps.executeUpdate()==0) throw new IllegalArgumentException("Informe o e-mail de uma conta de familiar cadastrada.");
                } catch(SQLException e) {
                    if ("23505".equals(e.getSQLState())) throw new IllegalArgumentException("Este familiar já está autorizado.");
                    throw e;
                }
            } else if(u instanceof Paciente && rota.equals("/revogar")) {
                gravar(c,"DELETE FROM vinculo_familiar WHERE paciente_id=? AND familiar_id=?",u.getId(),Integer.parseInt(texto(f,"id",10)));
            } else if(u instanceof Paciente && rota.equals("/entrega")) {
                String entrega="bloqueada".equals(f.get("notificacao"))?"NOTIFICACAO_BLOQUEADA":"EXIBIDO";
                Contas.executar(c,"UPDATE lembrete SET entrega=? WHERE id=? AND paciente_id=? AND aviso_em IS NOT NULL AND status<>'CONFIRMADO'",entrega,Integer.parseInt(texto(f,"id",12)),u.getId());
                x.sendResponseHeaders(204,-1);return;
            } else if(u.gerenciaRotina() && Set.of("/agendar","/editar","/desativar","/diario").contains(rota)) {
                int paciente=Integer.parseInt(f.getOrDefault("paciente","0"));
                if(!vinculado(c,paciente,u.getId())) throw new IllegalArgumentException("Selecione um paciente vinculado.");
                if(!rota.equals("/diario")) {
                    Programacoes.salvar(c,u,paciente,f,rota);
                } else {
                    var reg=new RegistroComportamental(0,paciente,texto(f,"categoria",32),Integer.parseInt(texto(f,"intensidade",1)),texto(f,"observacao",500));
                    gravar(c,"INSERT INTO registro_comportamental(paciente_id,categoria,intensidade,observacao,autor_id) VALUES(?,?,?,?,?)",paciente,reg.getCategoria(),reg.getIntensidade(),reg.getObservacao(),u.getId());
                }
            } else if(u instanceof Paciente && (rota.equals("/confirmar") || rota.equals("/adiar"))) {
                new LembreteDAO().responder(c,Integer.parseInt(texto(f,"id",10)),u.getId(),rota.equals("/adiar"));
            } else { pagina(x,403,"Operação indisponível para este perfil."); return; }
            if(!c.getAutoCommit()) c.commit();
            x.getResponseHeaders().set("Location",destino);
            x.sendResponseHeaders(303,-1);
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            pagina(x,400,e.getMessage());
        } catch(IllegalStateException e) {
            pagina(x,503,"Falha ao enviar o e-mail. Tente novamente.");
        } catch(SQLException e) {
            if("23505".equals(e.getSQLState())) pagina(x,409,"E-mail já cadastrado. Use Recuperar acesso na tela de entrada.");
            else if("23513".equals(e.getSQLState())) pagina(x,400,"Confira os valores do formulário.");
            else { e.printStackTrace(); pagina(x,500,"Falha ao consultar o banco."); }
        } finally { x.close(); }
    }
    private static void cadastrar(Connection c, Map<String,String> f, Usuario cuidador) throws SQLException {
        String nome=texto(f,"nome",120), email=Contas.email(f.getOrDefault("email",""));
        String perfil=cuidador==null ? f.getOrDefault("perfil","CUIDADOR") : "PACIENTE";
        if(!Set.of("CUIDADOR","FAMILIAR","PACIENTE").contains(perfil)) throw new IllegalArgumentException("Perfil inválido.");
        try(var ps=c.prepareStatement("INSERT INTO usuario(nome,email,senha_hash,perfil,cuidador_id,situacao) VALUES(?,?,'',?,?,'PENDENTE')",Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,nome); ps.setString(2,email); ps.setString(3,perfil);
            ps.setObject(4,cuidador==null?null:cuidador.getId());
            ps.executeUpdate();
            try(var ids=ps.getGeneratedKeys()) {
                if(!ids.next()) throw new SQLException("Falha ao criar conta.");
                Contas.emitir(c,ids.getInt(1),"ATIVAR",email);
            }
        }
    }
    private static void invalidar(int id) {
        sessoes.values().removeIf(s -> s.usuario().getId()==id);
    }
    private static Usuario login(Connection c,Map<String,String> f) throws SQLException {
        try(var p=c.prepareStatement("SELECT * FROM usuario WHERE email=? AND situacao='ATIVA'")) {
            p.setString(1,texto(f,"email",160).toLowerCase(Locale.ROOT));
            try(var r=p.executeQuery()) {
                if(r.next()) {
                    int id=r.getInt("id");
                    String nome=r.getString("nome"),email=r.getString("email"),hash=r.getString("senha_hash");
                    Usuario u = switch(r.getString("perfil")) {
                        case "CUIDADOR" -> new Cuidador(id,nome,email,hash);
                        case "PACIENTE" -> new Paciente(id,nome,email,hash);
                        case "FAMILIAR" -> new Familiar(id,nome,email,hash);
                        default -> throw new SQLException("Perfil desconhecido.");
                    };
                    if(u.autenticar(f.getOrDefault("senha",""))) return u;
                }
            }
        }
        throw new IllegalArgumentException("E-mail ou senha incorretos.");
    }
    private static boolean vinculado(Connection c,int paciente,int cuidador) throws SQLException {
        try(var p=c.prepareStatement("SELECT 1 FROM usuario WHERE id=? AND cuidador_id=? AND situacao='ATIVA'")) {
            p.setInt(1,paciente); p.setInt(2,cuidador);
            try(var r=p.executeQuery()) { return r.next(); }
        }
    }
    private static void gravar(Connection c,String sql,Object... valores) throws SQLException {
        try(var p=c.prepareStatement(sql)) {
            for(int i=0;i<valores.length;i++) p.setObject(i+1,valores[i]);
            p.executeUpdate();
        }
    }
    private static void pagina(HttpExchange x,int status,String conteudo) throws java.io.IOException {
        boolean texto = status >= 400 && "text/plain".equals(x.getRequestHeaders().getFirst("Accept"));
        String resposta = texto ? conteudo : Tela.documento(status >= 400 ? Tela.esc(conteudo) : conteudo,status>=400);
        byte[] html=resposta.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type",(texto ? "text/plain" : "text/html")+"; charset=utf-8");
        x.getResponseHeaders().set("Cache-Control","no-store");
        x.getResponseHeaders().set("Referrer-Policy","strict-origin");
        x.sendResponseHeaders(status,html.length);
        x.getResponseBody().write(html);
    }
}
