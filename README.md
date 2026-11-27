# Assistente de Rotina para Pessoas com Alzheimer

Projeto integrado das disciplinas de **Programação Orientada a Objetos**, **Banco de Dados** e **Engenharia de Software** (4º Período - Sistemas de Informação).

## Como abrir no IntelliJ IDEA
1. Extraia o arquivo `.zip`.
2. Abra o **IntelliJ IDEA** -> **File** -> **Open...** e selecione a pasta `assistente-alzheimer` (ou o arquivo `pom.xml`).
3. Aguarde o Maven importar as dependências.
4. Execute a classe `br.com.alzheimer.app.Main`.

## Estrutura do Repositório
- `src/main/java/br/com/alzheimer/model`: Entidades de domínio aplicando Herança, Polimorfismo, Encapsulamento e Abstração.
- `src/main/java/br/com/alzheimer/service`: Regras de negócio (Lembretes Inteligentes, Geolocalização e Monitoramento Comportamental).
- `src/main/java/br/com/alzheimer/repository`: Persistência com JDBC (`ConexaoBD` + DAOs).
- `docs/`: `prd.md` e `arquitetura.md`.
- `specs/`: Especificações detalhadas das funcionalidades.
- `database/`: `schema.sql` com DDL e estrutura relacional.
