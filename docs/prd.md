# PRD · CuidaMais Web

## Objetivo e público
O cuidador organiza atividades e registra ocorrências de pacientes vinculados. O paciente responde aos lembretes e controla o acesso dos familiares. O familiar autorizado consulta a agenda e o histórico de respostas.

## Sete funcionalidades do recorte acadêmico

| Funcionalidade | Requisitos de origem | Critério demonstrável |
|---|---|---|
| Cadastro e ativação | RF002, RF004 (perfil simplificado) | A conta fica pendente até o titular confirmar o e-mail. O paciente pode se cadastrar ou receber cadastro iniciado pelo cuidador. O nome do paciente fica fixo após o cadastro. |
| Autenticação e recuperação | RF003 | A redefinição de senha por link temporário de uso único encerra sessões anteriores. |
| Autorização do familiar | RF006 e RF016 (acesso simplificado) | Autorizar pelo e-mail libera a consulta da agenda e das respostas. Revogar bloqueia novas consultas. |
| Gestão de lembretes | RF001 e RF007 | Criar uma programação única, diária ou semanal, editar ou desativar. O painel exibe o aviso devido com instrução e voz opcional. Ocorrências emitidas conservam sua versão. |
| Resposta do paciente | RF008 e RF009 | Confirmar ou adiar uma vez por 15 minutos. A resposta altera a agenda e aparece no histórico. |
| Alerta de rotina | RF010 | Após 15 minutos sem confirmação, o cuidador vê o alerta. Uma confirmação tardia encerra o destaque e preserva o evento. |
| Registro de ocorrências | RF012 (parcial) | Gravar tipo, intensidade e observação e consultar o diário em ordem de lançamento. |

## Limites da versão acadêmica

A agenda permite filtrar por título, categoria, data, horário e estado. O histórico registra avisos, alertas e respostas. Os avisos aparecem no painel aberto. A autorização de familiares ocorre diretamente pelo paciente. Convites com aceite, exames, invalidação de lançamentos, alertas críticos de ocorrências, contatos de emergência, catálogo de cuidadores e relatórios por período ficam fora deste recorte. Geolocalização foi excluída na entrevista.

[Roteiro de demonstração](../README.md) · [Conta e vínculo](specs/0001-acesso.md) · [Agenda e respostas](specs/0002-rotina.md) · [Diário](specs/0003-diario.md)
