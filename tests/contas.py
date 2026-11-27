from pathlib import Path
import os,re,tempfile,subprocess,http.cookiejar,urllib.request as rq,urllib.parse as enc,urllib.error as err,socket
java=Path(os.environ["JAVA_HOME"])/"bin/java"
jar=Path("target/cuidarmais-1.0.0.jar").resolve()
def cliente():return rq.build_opener(rq.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def enviar(cli,rota,**dados):
    try:
        with cli.open(base+rota,enc.urlencode(dados).encode() if dados else None) as r:return r.status,r.read().decode()
    except err.HTTPError as e:return e.code,e.read().decode()
def ok(cli,rota,**dados):
    status,html=enviar(cli,rota,**dados)
    assert status==200,(rota,status,html[-500:])
    return html
def chave(email):
    for p in sorted(caixa.glob("*.txt"),reverse=True):
        s=p.read_text(encoding="utf8")
        if s.startswith("Para: "+email+"\n"):return re.search(r"token=([\w-]+)",s)[1]
    raise AssertionError(email)
def criar(email,perfil):
    cli=cliente()
    ok(cli,"/cadastro",nome="Teste",email=email,perfil=perfil)
    assert enviar(cli,"/entrar",email=email,senha="teste1234")[0]==400
    t=chave(email);assert "Ativar minha conta" in ok(cli,"/validar?token="+t)
    ok(cli,"/validar",token=t,senha="teste1234")
    assert enviar(cli,"/validar",token=t,senha="teste1234")[0]==400
    ok(cli,"/entrar",email=email,senha="teste1234")
    return cli
with tempfile.TemporaryDirectory() as pasta:
    caixa=Path(pasta)/"emails";db=str(Path(pasta)/"rotina")
    with socket.socket() as sock:sock.bind(("127.0.0.1",0));porta=sock.getsockname()[1]
    base=f"http://127.0.0.1:{porta}"
    env=os.environ.copy();env.pop("RESEND_API_KEY",None)
    p=subprocess.Popen([str(java),"-Dcuida.porta="+str(porta),"-Dcuida.caixa="+str(caixa),"-Dcuida.banco="+db,"-jar",str(jar)],env=env,stdout=subprocess.PIPE)
    try:
        assert b"localhost" in p.stdout.readline()
        a=criar("cuida@example.invalid","CUIDADOR")
        b=criar("pac@example.invalid","PACIENTE")
        f=criar("fam@example.invalid","FAMILIAR")
        assert "action='/nome'" not in ok(b,"/")
        assert enviar(b,"/nome",nome="Alterado")[0]==403
        assert "Rotina de Teste" in ok(b,"/")
        ok(b,"/transferir",email="cuida@example.invalid",senha="teste1234")
        assert enviar(a,"/encerrar",senha="teste1234",confirmar="sim")[0]==400
        ok(b,"/autorizar",email="fam@example.invalid")
        ok(a,"/agendar",repeticao="UNICA",paciente="2",titulo="Atividade de teste",categoria="Higiene",horario="2026-11-27T08:00")
        from datetime import datetime,timedelta
        import base64,time
        hoje=datetime.now().date()
        agora=datetime.now().replace(second=0,microsecond=0).isoformat(timespec="minutes")
        serie=dict(paciente=2,titulo="Café original",categoria="Alimentação",horario=agora,instrucao="Instrução original",repeticao="DIARIA")
        mp3=(bytes.fromhex("fffb9000")+bytes(413))*2
        ok(a,"/agendar",**serie,gravacao=base64.b64encode(mp3).decode())
        for _ in range(20):
            avisos=ok(b,"/").split("id='avisos'",1)[1].split("</section>",1)[0]
            if "Instrução original" in avisos:break
            time.sleep(.3)
        assert "🍽" in avisos and "Instrução original" in avisos
        som=re.search(r"src='(/voz\?id=(\d+))'",avisos)
        assert som and b.open(base+som[1]).read()==mp3
        assert enviar(f,som[1])[0]==404
        assert enviar(b,"/editar",**serie,programa=2,revisao=1)[0]==403
        assert enviar(a,"/editar",**dict(serie,repeticao="SEMANAL"),programa=2,revisao=1)[0]==400
        assert enviar(a,"/editar",**serie,programa=2,revisao=1,gravacao="AAAA")[0]==400
        amanha=hoje+timedelta(days=1)
        editada=dict(serie,titulo="Café atualizado",horario=str(amanha)+"T08:30",instrucao="Nova instrução")
        ok(a,"/editar",**editada,programa=2,revisao=1,retirar="on")
        assert b.open(base+som[1]).read()==mp3
        consulta="/?"+enc.urlencode(dict(titulo="atualizado",hora="08:30",data=str(amanha),categoria="Alimentação",estado="PENDENTE"))
        itens=lambda:ok(b,consulta).split("id='itens-agenda'>",1)[1].split("</div>",1)[0]
        assert itens().count("Café atualizado")==1
        ok(b,"/confirmar",id=som[2])
        ok(a,"/desativar",paciente=2,programa=2,revisao=2)
        assert "Nenhum lembrete" in itens()
        assert "Café original" in ok(b,"/").split("id='historico'",1)[1]
        semanal=dict(serie,repeticao="SEMANAL",titulo="Semanal",horario=str(amanha)+"T10:30")
        semanal["dia"+str(amanha.isoweekday())]="on"
        ok(a,"/agendar",**semanal)
        assert "Semanal" in ok(b,"/?data="+str(amanha)).split("id='itens-agenda'>",1)[1].split("</div>",1)[0]
        assert "Semanal" not in ok(b,"/?data="+str(amanha+timedelta(days=1))).split("id='itens-agenda'>",1)[1].split("</div>",1)[0]
        print("OK: recorrencia, filtros, voz autorizada, alteracao com historico, desativacao e validacoes.")
        assert "Atividade de teste" in ok(f,"/")
        ok(f,"/novo-email",email="novo@example.invalid",senha="teste1234")
        tmp=cliente()
        ok(tmp,"/entrar",email="fam@example.invalid",senha="teste1234")
        ok(cliente(),"/validar",token=chave("novo@example.invalid"))
        assert "Acompanhe sua rotina" in ok(f,"/")
        assert enviar(cliente(),"/entrar",email="fam@example.invalid",senha="teste1234")[0]==400
        msg=ok(cliente(),"/recuperar",email="fantasma@example.invalid")
        assert msg==ok(cliente(),"/recuperar",email="novo@example.invalid")
        anterior=chave("novo@example.invalid")
        ok(cliente(),"/recuperar",email="novo@example.invalid")
        assert enviar(cliente(),"/validar?token="+anterior)[0]==400
        t=chave("novo@example.invalid")
        ok(f,"/entrar",email="novo@example.invalid",senha="teste1234")
        ok(cliente(),"/validar",token=t,senha="outrasenha")
        assert "Acompanhe sua rotina" in ok(f,"/")
        assert enviar(cliente(),"/validar",token=t,senha="terceirasenha")[0]==400
        assert enviar(f,"/entrar",email="novo@example.invalid",senha="teste1234")[0]==400
        ok(f,"/entrar",email="novo@example.invalid",senha="outrasenha")
        ok(f,"/nome",nome="Familiar editado")
        assert "Familiar editado" in ok(f,"/entrar",email="novo@example.invalid",senha="outrasenha")
        ok(f,"/encerrar",senha="outrasenha",confirmar="sim")
        assert enviar(f,"/entrar",email="novo@example.invalid",senha="outrasenha")[0]==400
        assert "Familiar editado" not in ok(b,"/")
        ok(b,"/encerrar",senha="teste1234",confirmar="sim")
        ok(a,"/encerrar",senha="teste1234",confirmar="sim")
    finally:
        p.terminate();p.wait()
    def sql(comando):
        return subprocess.check_output([str(java),"-cp",str(jar),"org.h2.tools.Shell","-url","jdbc:h2:file:"+db,"-user","sa","-password","","-sql",comando],text=True)
    assert "cuida" not in sql("SELECT email FROM usuario WHERE id=1")
    assert "Teste" in sql("SELECT u.nome FROM lembrete l JOIN usuario u ON u.id=l.autor_id WHERE u.situacao='ENCERRADA'")
    import hashlib
    expirado="teste-expirado"
    digest=hashlib.sha256(expirado.encode()).hexdigest()
    sql("INSERT INTO acao_conta VALUES('"+digest+"',1,'SENHA','teste@example.invalid',DATEADD(MINUTE,-1,CURRENT_TIMESTAMP))")
    p=subprocess.Popen([str(java),"-Dcuida.porta="+str(porta),"-Dcuida.banco="+db,"-jar",str(jar)],env=env,stdout=subprocess.PIPE)
    try:
        assert b"localhost" in p.stdout.readline()
        assert enviar(cliente(),"/validar?token="+expirado)[0]==400
        assert enviar(cliente(),"/validar",token=expirado,senha="nova-senha")[0]==400
    finally:
        p.terminate();p.wait()
    print("OK: ativação, três perfis, vínculo, nome, mudança de e-mail, recuperação, revogação de sessões, encerramento protegido.")

