# Arquitetura do Sistema

## Camadas (POO + Persistência)
- `br.com.alzheimer.model`: Entidades de domínio (`Usuario`, `Cuidador`, `Paciente`, `Lembrete`, `ZonaSegura`, `RegistroComportamental`) e contratos (`Notificavel`).
- `br.com.alzheimer.service`: Regras de negócio de geolocalização e monitoramento comportamental.
- `br.com.alzheimer.repository`: Acesso a dados via JDBC (`ConexaoBD`, `LembreteDAO`).
- `br.com.alzheimer.app`: Ponto de entrada e simulação dos fluxos operacionais (`Main`).
