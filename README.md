# CuidaMais

Aplicação web acadêmica de Arthur Felipe Bezerra de Almeida e Jorge José Luiz Pacheco da Silva. O cuidador organiza a rotina e registra ocorrências. O paciente entra com sua própria conta para consultar a agenda, confirmar tarefas e adiar uma vez. O familiar autorizado acompanha a agenda e o histórico de respostas.

## Executar no computador da apresentação

Requer JDK 17 ou superior e Maven 3.9. Na raiz do projeto:

```powershell
mvn clean verify
java -jar target/cuidarmais-1.0.0.jar
```

Abra http://localhost:8080. O primeiro acesso cria o banco H2 em `dados/rotina.mv.db`. Reinicie na mesma pasta para recuperar os dados. O servidor escuta somente no computador local. Utilize dados fictícios. O JAR compilado também pode ser executado diretamente em outro computador com JDK 17.

## Sete funcionalidades para demonstrar na AV03

Use e-mails fictícios diferentes para cuidador, paciente e familiar. Na configuração local, abra o link de ativação ou recuperação no TXT do destinatário em `dados/emails/`.

1. **Cadastro e ativação.** Cadastre e ative o cuidador. No painel dele, abra **Cadastrar paciente vinculado** e confirme a conta pelo e-mail do paciente. Cadastre e ative também o familiar na tela inicial.
2. **Autenticação e recuperação.** Em **Recuperar acesso ou reenviar confirmação**, solicite um link para o cuidador, escolha uma nova senha e entre com ela.
3. **Autorização do familiar.** Como paciente, abra **Acesso da família** e autorize o e-mail do familiar. Depois de criar os lembretes, entre como familiar para consultar a agenda. Revogue o vínculo pelo painel do paciente e confira que o acesso termina.
4. **Gestão de lembretes.** Como cuidador, cadastre uma tarefa diária e outra única para o minuto atual. Em **Gerenciar programações**, altere o horário da tarefa diária e confira na agenda que a alteração vale para as ocorrências futuras.
5. **Resposta do paciente.** No painel do paciente, confirme uma das tarefas e escolha **Lembrar em 15 min** na outra. Veja as duas respostas no histórico.
6. **Alerta por falta de confirmação.** Como cuidador, cadastre uma terceira tarefa única com horário de 16 minutos atrás. Observe o alerta, confirme a tarefa como paciente e confira que o destaque desaparece e o evento continua no histórico.
7. **Registro de ocorrências.** Como cuidador, registre uma queda fictícia com intensidade e observação. Consulte o lançamento no diário do paciente. Reinicie na mesma pasta e entre novamente para demonstrar que os dados foram preservados.

[PRD](docs/prd.md) · [Arquitetura e POO](docs/arquitetura.md) · [Especificações](docs/specs/) · [Entrega de banco em TXT](docs/entrega-banco.txt) · [SQL executável](database/schema.sql)

## Perfil familiar

Na tela inicial, cadastre e ative uma conta do tipo Familiar. Entre como paciente e, em **Acesso da família**, autorize o e-mail cadastrado. Ao entrar, o familiar pode consultar a agenda e o histórico de respostas dos pacientes que o autorizaram. O diário é restrito ao cuidador vinculado e ao paciente. O paciente pode revogar o acesso pelo mesmo painel.

## Demonstrar o alerta de rotina

Agende um lembrete único para agora e mantenha o painel do paciente aberto. O aviso aparece automaticamente. Um adiamento às 08:05 muda o horário para 08:20. Se o paciente ainda não confirmou às 08:35, aparece um alerta no painel do cuidador. Uma confirmação posterior encerra o destaque e preserva o horário do alerta no histórico. Para demonstrar o alerta rapidamente, cadastre outro lembrete com horário de 16 minutos atrás.

## Confirmação de conta e recuperação de senha

Na demonstração local, cada envio gera uma mensagem em `dados/emails/`, pasta privada do computador e fora do Git. Abra o TXT mais recente do destinatário e copie o link para o navegador. O paciente também pode se cadastrar diretamente. No cadastro a conta fica pendente e o titular define a senha ao confirmar o endereço. Contas existentes antes desta atualização continuam ativas.

Na entrada, **Recuperar acesso ou reenviar confirmação** atende os dois casos. Cada link dura 30 minutos, é consumido por POST e aceita um único uso. Solicitar um novo link para a mesma operação substitui o anterior. A troca de senha encerra as sessões anteriores. Em **Minha conta** os três perfis podem trocar o e-mail e solicitar encerramento. Apenas cuidador e familiar podem editar o próprio nome. O paciente pode indicar outro cuidador ativo antes do encerramento do cuidador atual.

Para entregar mensagens por e-mail real, configure `RESEND_API_KEY` e `CUIDA_REMETENTE` (remetente verificado no Resend) antes de iniciar o JAR. Configure `CUIDA_URL` com a URL confiável da aplicação acessível aos destinatários. O padrão é localhost e o servidor local escuta na interface de loopback. Sem chave, o envio fica na caixa de demonstração. Os links são credenciais temporárias: não compartilhe essa pasta.

## Demonstrar recorrência e versão do lembrete

Como cuidador, escolha **Todos os dias**, inclua uma instrução e, se desejar, anexe uma gravação MP3. Para observar o primeiro aviso, programe o minuto atual. Entre como paciente para ler a instrução e reproduzir a voz no painel. Em **Gerenciar programações**, o cuidador pode alterar o horário ou desativar a programação. Use os filtros da agenda para conferir os horários futuros e o histórico para conferir os avisos já emitidos.
