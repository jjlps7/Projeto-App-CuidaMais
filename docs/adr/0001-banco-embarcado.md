# 0001 · Banco relacional embarcado

**Estado** Aceita.

**Contexto** O protótipo gravava em SQLite, mas o arquivo SQL usava sintaxe MySQL. A disciplina exige contas e permissões reais de banco.

**Decisão** Usar H2 via JDBC, em arquivo, com um único DDL. O banco admite trigger, índice, view, usuários e GRANT. O arquivo SQLite de uma execução anterior não é importado.

**Consequência** A demonstração precisa de um JDK e do JAR, dispensando um serviço de banco separado.
