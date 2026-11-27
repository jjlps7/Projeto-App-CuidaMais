# Arquitetura do CuidaMais

Uma aplicação Java 17 em um módulo Maven serve HTML pelo HttpServer do JDK em localhost. Os formulários enviam POST e o servidor redireciona para a agenda. A aplicação executa SQL explícito por JDBC e persiste os dados em H2 em arquivo. O mesmo schema executado no primeiro acesso serve para a avaliação de BD.

## Responsabilidades e pilares de POO
| Pilar | Evidência no fluxo |
|---|---|
| Abstração | Main autentica e guarda um Usuario na sessão. O contrato gerenciaRotina define se ele pode administrar uma rotina. |
| Herança | Paciente, Familiar e Cuidador herdam identidade, nome e verificação de senha de Usuario. |
| Polimorfismo | A chamada gerenciaRotina no servidor e na tela executa a implementação do perfil concreto e controla o acesso aos formulários e operações. |
| Encapsulamento | Lembrete.responder controla estado e horário privados, bloqueia a resposta antes do primeiro aviso, permite confirmar durante o adiamento e impede um segundo adiamento. O DAO bloqueia a linha até concluir a transação. |

## Caminho de uma resposta
Paciente → formulário → Main → LembreteDAO → Lembrete.responder → UPDATE e trigger na mesma transação → histórico no navegador.

## Persistência e acesso
A ativação da conta grava hash PBKDF2 com sal individual. A sessão tem token aleatório, cookie HttpOnly e duração de quatro horas. As consultas verificam a identidade do paciente, o vínculo com o cuidador ou a autorização concedida ao familiar, conforme a operação. O administrador prepara o banco local. A aplicação abre conexões com rotina_app e a conta consulta só pode ler a view agregada.

## Skills aplicadas
find-skills orientou a busca. 121-java-object-oriented-design orientou a revisão da hierarquia e das transições de estado. 110-java-maven-best-practices orientou o uso de um módulo, layout convencional, versões explícitas e empacotamento. agent-browser foi utilizada para testar a interface e o fluxo de saída no navegador.

## Perfil familiar

Na tela inicial, cadastre uma conta do tipo Familiar. Entre como paciente e, em **Acesso da família**, autorize o e-mail cadastrado. Ao entrar, o familiar pode consultar a agenda e o histórico de respostas dos pacientes que o autorizaram. O diário é restrito ao cuidador vinculado e ao paciente. O paciente pode revogar o acesso pelo mesmo painel.
