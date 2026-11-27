package br.com.alzheimer.app;

import br.com.alzheimer.model.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

final class Tela {
    private static final DateTimeFormatter DATA=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static String esc(String s) {
        return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
    static String documento(String corpo,boolean erro) {
        return """
        <!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
        <title>CuidaMais · Rotina</title><style>
        *{box-sizing:border-box}body{margin:0;background:#f5f4ed;color:#183a34;font:17px/1.5 'Segoe UI',sans-serif}
        header{background:#153e36;color:white;padding:18px max(5vw,24px);font-size:24px;font-weight:700}
        main{max-width:940px;margin:36px auto;padding:0 20px}h1,h2{line-height:1.2}h1{font-family:Georgia,serif;font-size:38px}
        section,article{padding:20px;margin:14px 0;background:white;border:1px solid #cbd6cb;border-radius:12px}
        label{display:grid;gap:3px;margin-bottom:10px}input,select,button{font:inherit;padding:9px;border-radius:6px;border:1px solid #789387}
        button{background:#185443;color:white;font-weight:600;cursor:pointer}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,260px),1fr));gap:14px}
        details{margin:12px 0}summary{cursor:pointer;font-weight:650}article form{display:inline-block;margin:5px}a{color:#125343}
        #avisos:empty{display:none}#avisos{border-left:5px solid #935323}
        .erro-formulario{color:#85261b;font-weight:600}
        </style><header>CuidaMais</header><main>
        """ + (erro?"<h1>Confira a operação</h1><p>"+corpo+"</p><a href='/'>Voltar ao início</a>":corpo)+"</main>"+SCRIPT_CONTA+"</html>";
    }
    private static final String SCRIPT_CONTA = """
        <script>
        async function atualizarAgenda() {
            try {
                const resposta = await fetch(location.href, {cache:'no-store'});
                if (!resposta.ok) return;
                const pagina = new DOMParser().parseFromString(await resposta.text(),'text/html');
                if (!pagina.getElementById('avisos')) { location.replace('/'); return; }
                for (const id of ['avisos','itens-agenda','historico']) {
                    const atual=document.getElementById(id), novo=pagina.getElementById(id);
                    if (atual.innerHTML !== novo.innerHTML) atual.replaceChildren(...novo.childNodes);
                }
                registrarAvisos();
            } catch { /* A próxima consulta retoma a atualização. */ }
            finally { setTimeout(atualizarAgenda,3000); }
        }
        if(document.getElementById('avisos')) setTimeout(atualizarAgenda,3000);
        document.addEventListener('change', e => {
            if(e.target.name==='repeticao') e.target.form.querySelector('.dias').hidden=e.target.value!=='SEMANAL';
        });
        const entregues=new Set();
        async function registrarAvisos(){
            if(document.visibilityState!=='visible') return;
            for(const el of document.querySelectorAll('[data-aviso]')){
                const bloqueada=window.Notification?.permission==='denied';
                const chave=el.dataset.aviso+el.dataset.horario+bloqueada;
                if(entregues.has(chave)) continue;
                entregues.add(chave);
                try {
                    const r=await fetch('/entrega',{method:'POST',body:new URLSearchParams({id:el.dataset.aviso,notificacao:bloqueada?'bloqueada':'painel'})});
                    if(!r.ok) entregues.delete(chave);
                } catch { entregues.delete(chave); }
            }
        }
        registrarAvisos();
        document.addEventListener('visibilitychange',registrarAvisos);
        document.addEventListener('submit', async event => {
            const form = event.target;
            const aviso = form.querySelector('.erro-formulario');
            if (!aviso) return;
            event.preventDefault();
            const botao = form.querySelector('button');
            if (botao.disabled) return;
            botao.disabled = true;
            aviso.textContent = '';
            try {
                const dados=new FormData(form);
                const arquivo=dados.get('arquivo');
                dados.delete('arquivo');
                if(arquivo && arquivo.size){
                    if(arquivo.size>2097152){aviso.textContent='Selecione um MP3 de até 2 MB.';return;}
                    const leitor=new FileReader();
                    const url=await new Promise((ok,erro)=>{leitor.onload=()=>ok(leitor.result);leitor.onerror=erro;leitor.readAsDataURL(arquivo);});
                    dados.set('gravacao',url.split(',')[1]);
                }
                const resposta = await fetch(form.action, {
                    method: 'POST',
                    headers: {Accept: 'text/plain'},
                    body: new URLSearchParams(dados)
                });
                if (resposta.redirected && resposta.ok) {
                    location.replace(resposta.url);
                    return;
                }
                aviso.textContent = await resposta.text();
                if (form.elements.senha) form.elements.senha.value = '';
                aviso.focus();
            } catch {
                aviso.textContent = 'Falha de conexão. Confira a conexão e tente novamente.';
                aviso.focus();
            } finally {
                botao.disabled = false;
            }
        });
        </script>
        """;
    static String campo(String label,String nome,String tipo) {
        return "<label>"+label+"<input required name='"+nome+"' type='"+tipo+"'></label>";
    }
    static String form(String rota,String campos,String botao) {
        boolean conta = java.util.Set.of("agendar","editar","desativar").contains(rota) || rota.equals("entrar") || rota.equals("cadastro") || rota.equals("paciente") || rota.equals("autorizar") || java.util.Set.of("validar","recuperar","nome","novo-email","encerrar","transferir").contains(rota);
        String aviso = conta ? "<div class='erro-formulario' role='alert' tabindex='-1'></div>" : "";
        return "<form method='post' action='/"+rota+"'>"+campos+aviso+"<button>"+botao+"</button></form>";
    }
    private static final java.util.List<String> INTENSIDADES =
        java.util.List.of("Muito baixa", "Baixa", "Moderada", "Alta", "Muito alta");

    private static String legendaIntensidade(int nivel) {
        return nivel + " – " + INTENSIDADES.get(nivel - 1);
    }

    private static String opcoesIntensidade() {
        var html = new StringBuilder();
        for (int nivel = 1; nivel <= INTENSIDADES.size(); nivel++) {
            html.append("<option value='").append(nivel).append("'>")
                .append(legendaIntensidade(nivel)).append("</option>");
        }
        return html.toString();
    }

    static String opcoes(String... itens) {
        StringBuilder b=new StringBuilder();
        for(String i:itens)b.append("<option>").append(esc(i)).append("</option>");
        return b.toString();
    }
    private static String conta() {
        return campo("Nome","nome","text")+campo("E-mail","email","email");
    }
    static String entrada() {
        return "<h1>Acompanhe sua rotina</h1><div class='grid'><section><h2>Entrar</h2>"+
            form("entrar",campo("E-mail","email","email")+campo("Senha","senha","password"),"Entrar")+
            "</section><section><h2>Criar conta</h2>"+form("cadastro","<label>Tipo de conta<select name='perfil'><option value='CUIDADOR'>Cuidador</option><option value='FAMILIAR'>Familiar</option><option value='PACIENTE'>Paciente</option></select></label>"+conta(),"Cadastrar")+"</section></div><details><summary>Recuperar acesso ou reenviar confirmação</summary>"+form("recuperar",campo("E-mail","email","email"),"Solicitar link")+"</details>";
    }
    static String avisoConta(String codigo) {
        String texto=switch(codigo) {
            case "cadastro" -> "Cadastro recebido. Abra o link enviado ao e-mail para definir a senha e ativar a conta.";
            case "recuperar" -> "Se houver uma conta elegível, você receberá um link por e-mail.";
            case "validado" -> "Alteração confirmada. Entre com suas credenciais.";
            case "email" -> "Confira o novo endereço de e-mail para confirmar a troca.";
            case "nome" -> "Nome atualizado. Entre novamente.";
            case "encerrada" -> "Conta encerrada.";
            case "transferido" -> "Cuidador autorizado para acompanhar sua rotina.";
            default -> "";
        };
        return texto.isEmpty()?"":"<p role='status'>"+texto+"</p>";
    }
    static String validar(Connection c,Contas.Acao acao,String token) throws SQLException {
        String titulo=switch(acao.tipo()) {
            case "ATIVAR" -> "Ativar minha conta";
            case "EMAIL" -> "Confirmar novo e-mail";
            default -> "Redefinir minha senha";
        };
        String campos="<input type='hidden' name='token' value='"+esc(token)+"'>";
        if(!acao.tipo().equals("EMAIL")) campos+=campo("Defina uma senha de 8 a 128 caracteres","senha","password");
        String detalhe="";
        if(acao.tipo().equals("ATIVAR")) {
            try(var p=c.prepareStatement("SELECT u.nome,u.perfil,c.nome AS cuidador,c.email AS contato FROM usuario u LEFT JOIN usuario c ON c.id=u.cuidador_id WHERE u.id=?")) {
                p.setInt(1,acao.usuario());
                try(var r=p.executeQuery()) { if(r.next()) {
                    detalhe="<p>"+esc(r.getString("nome"))+" · "+esc(r.getString("perfil"))+"</p>";
                    if(r.getString("cuidador")!=null) detalhe+="<p>Ao ativar, você autoriza "+esc(r.getString("cuidador"))+" ("+esc(r.getString("contato"))+") a acompanhar sua rotina.</p>";
                }}
            }
        }
        return "<h1>"+titulo+"</h1><p>"+esc(acao.email())+"</p>"+detalhe+form("validar",campos,titulo);
    }
    static String painel(Connection c,Usuario u,java.util.Map<String,String> filtros) throws SQLException {
        String estado=filtros.getOrDefault("estado","");
        boolean cuidador=u.gerenciaRotina();
        var b=new StringBuilder("<h1>Rotina de "+esc(u.getNome())+"</h1><p>"+esc(u.getPerfil())+"</p>");
        b.append("<details><summary>Minha conta</summary>");
        if (!(u instanceof Paciente)) b.append(form("nome","<label>Nome<input name='nome' required maxlength='120' value=\""+esc(u.getNome())+"\"></label>","Salvar nome"));
        b.append(form("novo-email",campo("Novo e-mail","email","email")+campo("Senha atual","senha","password"),"Confirmar novo e-mail"));
        if(u instanceof Paciente) b.append(form("transferir",campo("E-mail do cuidador que terá acesso à sua rotina","email","email")+campo("Senha atual","senha","password"),"Autorizar cuidador"));
        b.append(form("encerrar",campo("Senha atual","senha","password")+"<label><input type='checkbox' name='confirmar' value='sim' required>Confirmo o encerramento e a revogação dos meus vínculos</label>","Encerrar minha conta")).append("</details>");
        String escopo=cuidador?"u.cuidador_id=?":"u.id=?";
        if (u instanceof Familiar) {
            escopo="EXISTS (SELECT 1 FROM vinculo_familiar v WHERE v.paciente_id=u.id AND v.familiar_id=?)";
            b.append("<p>Para acompanhar uma rotina, peça ao paciente que autorize seu e-mail no painel dele.</p>");
        }
        if (u instanceof Paciente) {
            b.append("<section><h2>Acesso da família</h2><p>Autorize a consulta da sua agenda e do histórico de respostas.</p>")
                .append(form("autorizar",campo("E-mail do familiar cadastrado","email","email"),"Autorizar familiar"));
            try (var p=c.prepareStatement("SELECT u.id,u.nome,u.email FROM vinculo_familiar v JOIN usuario u ON u.id=v.familiar_id WHERE v.paciente_id=? ORDER BY u.nome")) {
                p.setInt(1,u.getId());
                try(var r=p.executeQuery()) {
                    while(r.next()) b.append("<article>").append(esc(r.getString("nome"))).append(" · ")
                        .append(esc(r.getString("email"))).append(form("revogar","<input type='hidden' name='id' value='"+r.getInt("id")+"'>","Revogar acesso")).append("</article>");
                }
            }
            b.append("</section>");
        }
        if(cuidador) {
            b.append("<details><summary>Cadastrar paciente vinculado</summary>").append(form("paciente",conta(),"Vincular paciente")).append("</details>");
            var lista=new StringBuilder();
            try(var p=c.prepareStatement("SELECT id,nome FROM usuario WHERE cuidador_id=? AND situacao='ATIVA' ORDER BY nome,id")) {
                p.setInt(1,u.getId());
                try(var r=p.executeQuery()){while(r.next()) lista.append("<option value='").append(r.getInt(1)).append("'>").append(esc(r.getString(2))).append("</option>");}
            }
            b.append(TelaProgramacao.gerenciar(c,u.getId()));
            if(lista.isEmpty()) b.append("<p>Cadastre um paciente para começar a organizar a rotina.</p>");
            else {
                String pacientes="<label>Paciente<select name='paciente'>"+lista+"</select></label>";
                b.append("<div class='grid'><section><h2>Novo lembrete</h2>").append(form("agendar",pacientes+TelaProgramacao.campos(null),"Agendar"))
                    .append("</section><section><h2>Registrar ocorrência</h2>").append(form("diario",pacientes+
                    "<label>Tipo<select name='categoria'>"+opcoes("Queda","Desmaio","Esquecimento","Observação")+"</select></label><label>Intensidade<select name='intensidade'>"+opcoesIntensidade()+"</select></label>"+campo("Observação","observacao","text"),"Salvar no diário")).append("</section></div>");
            }
        }
        b.append("<section id='avisos' aria-live='polite'>");
        String avisoSql = "SELECT l.id,l.status,l.categoria,l.titulo,u.nome,l.agendado,l.alerta_em,l.instrucao,l.entrega,v.voz_id FROM agenda_rotina l LEFT JOIN versao_rotina v ON v.programa=l.programa AND v.revisao=l.revisao JOIN usuario u ON u.id=l.paciente_id WHERE "+escopo
            +" AND l.status<>'CONFIRMADO' AND "+(u instanceof Paciente ? "((l.status='PENDENTE' AND l.aviso_em IS NOT NULL) OR l.reaviso_em IS NOT NULL)" : "l.alerta_em IS NOT NULL")+" ORDER BY l.agendado,l.id";
        try(var p=c.prepareStatement(avisoSql)) {
            p.setInt(1,u.getId());
            try(var r=p.executeQuery()) {
                while(r.next()) {
                    if(r.isFirst()) b.append(u instanceof Paciente ? "<h2>Lembretes agora</h2>" : "<h2>Alertas de rotina</h2>");
                    String categoria=r.getString("categoria");
                    String icone=switch(categoria){case "Medicamento" -> "💊";case "Alimentação" -> "🍽";case "Higiene" -> "🧼";default -> "🚶";};
                    b.append("<p").append(u instanceof Paciente?" data-aviso='"+r.getInt("id")+"' data-horario='"+r.getTimestamp("agendado")+"'":"").append("><span aria-hidden='true'>").append(icone).append("</span> ").append(esc(categoria)).append(" · <strong>").append(esc(r.getString("titulo"))).append("</strong> · ").append(esc(r.getString("nome")))
                        .append(" · ").append(DATA.format(r.getObject("agendado",LocalDateTime.class)));
                    if(r.getTimestamp("alerta_em")!=null) b.append(" · Sem confirmação desde ").append(DATA.format(r.getObject("alerta_em",LocalDateTime.class)));
                    b.append("<br>").append(esc(r.getString("instrucao"))).append("</p>");
                    if (u instanceof Paciente) {
                        if(r.getObject("voz_id")!=null) b.append("<label>Voz familiar<audio controls preload='none' src='/voz?id=").append(r.getInt("id")).append("'></audio></label>");
                        String id="<input type='hidden' name='id' value='"+r.getInt("id")+"'>";
                        b.append(form("confirmar",id,r.getString("categoria").equals("Medicamento")?"Já tomei":"Já fiz"));
                        if (r.getString("status").equals("PENDENTE") && LocalDateTime.now().isBefore(r.getObject("agendado",LocalDateTime.class).plusMinutes(15)))
                            b.append(form("adiar",id,"Lembrar em 15 min"));
                    }
                }
            }
        }
        b.append("</section><section><h2>Agenda</h2><form><label>Estado<select name='estado'><option value=''>Todos</option>");
        for(String s:new String[]{"PENDENTE","ADIADO","ALERTA","CONFIRMADO"}) b.append("<option ").append(s.equals(estado)?"selected":"").append(">").append(s).append("</option>");
        b.append("</select></label>");
        String[] chaves={"titulo","categoria","data","hora"};
        String[] rotulos={"Título","Categoria","Data","Horário"};
        String[] tipos={"text","text","date","time"};
        for(int i=0;i<chaves.length;i++) b.append("<label>").append(rotulos[i]).append("<input type='").append(tipos[i]).append("' name='").append(chaves[i]).append("' value=\"").append(esc(filtros.get(chaves[i]))).append("\"></label>");
        b.append("<button>Filtrar</button></form><div id='itens-agenda'>");
        String sql="SELECT l.*,u.nome FROM agenda_rotina l JOIN usuario u ON u.id=l.paciente_id WHERE "+escopo+" AND (?='' OR l.estado_atual=?) AND LOCATE(LOWER(?),LOWER(l.titulo))>0 AND LOCATE(LOWER(?),LOWER(l.categoria))>0 AND (?='' OR FORMATDATETIME(l.agendado,'yyyy-MM-dd')=?) AND (?='' OR FORMATDATETIME(l.agendado,'HH:mm')=?) ORDER BY l.agendado,l.id";
        try(var p=c.prepareStatement(sql)) {
            p.setInt(1,u.getId());p.setString(2,estado);p.setString(3,estado);
            p.setString(4,filtros.getOrDefault("titulo",""));p.setString(5,filtros.getOrDefault("categoria",""));
            String dia=filtros.getOrDefault("data",""),hora=filtros.getOrDefault("hora","");
            p.setString(6,dia);p.setString(7,dia);p.setString(8,hora);p.setString(9,hora);
            try(var r=p.executeQuery()){
                boolean encontrou=false;
                while(r.next()){
                    encontrou=true;
                    var data=r.getObject("agendado",LocalDateTime.class);
                    String status=r.getString("estado_atual");
                    b.append("<article><b>").append(esc(r.getString("titulo"))).append("</b><br>").append(esc(r.getString("nome"))).append(" · ").append(DATA.format(data)).append(" · ").append(esc(r.getString("categoria"))).append(" · ").append(status);
                    if(u instanceof Paciente && status.equals("ADIADO")){
                        String id="<input type='hidden' name='id' value='"+r.getInt("id")+"'>";
                        b.append("<br>").append(form("confirmar",id,r.getString("categoria").equals("Medicamento")?"Já tomei":"Já fiz"));
                    }
                    b.append("</article>");
                }
                if(!encontrou)b.append("<p>Nenhum lembrete para este filtro.</p>");
            }
        }
        b.append("</div>");
        if (!(u instanceof Familiar)) {
            b.append("</section><section><h2>Diário</h2>");
            try(var p=c.prepareStatement("SELECT u.nome,d.* FROM registro_comportamental d JOIN usuario u ON u.id=d.paciente_id WHERE "+escopo+" ORDER BY d.registrado_em DESC,d.id DESC")){
                p.setInt(1,u.getId());try(var r=p.executeQuery()){
                    if(!r.isBeforeFirst())b.append("<p>Nenhuma ocorrência registrada.</p>");
                    while(r.next())b.append("<article>").append(DATA.format(r.getObject("registrado_em",LocalDateTime.class))).append(" · ").append(esc(r.getString("nome"))).append(" · ").append(esc(r.getString("categoria"))).append(" · Intensidade ").append(legendaIntensidade(r.getInt("intensidade"))).append("<br>").append(esc(r.getString("observacao"))).append("</article>");
                }
            }
        }
        b.append("</section><section id='historico'><h2>Histórico de respostas</h2>");
        try(var p=c.prepareStatement("SELECT l.titulo,u.nome,h.estado,h.registrado_em FROM (SELECT lembrete_id,estado,registrado_em FROM historico UNION ALL SELECT id,'AVISO PROGRAMADO',COALESCE(previsto,aviso_em) FROM lembrete WHERE aviso_em IS NOT NULL UNION ALL SELECT id,'REAVISO PROGRAMADO',reaviso_em FROM lembrete WHERE reaviso_em IS NOT NULL UNION ALL SELECT id,'ALERTA SEM CONFIRMAÇÃO',alerta_em FROM lembrete WHERE alerta_em IS NOT NULL) h JOIN lembrete l ON l.id=h.lembrete_id JOIN usuario u ON u.id=l.paciente_id WHERE "+escopo+" ORDER BY h.registrado_em DESC")){
            p.setInt(1,u.getId());try(var r=p.executeQuery()){
                if(!r.isBeforeFirst())b.append("<p>Nenhuma resposta registrada.</p>");
                while(r.next())b.append("<p>").append(DATA.format(r.getObject(4,LocalDateTime.class))).append(" · ").append(esc(r.getString(2))).append(" · ").append(esc(r.getString(1))).append(" · ").append(esc(r.getString(3))).append("</p>");
            }
        }
        return b.append("</section>").append(form("sair","","Sair")).toString();
    }
}
