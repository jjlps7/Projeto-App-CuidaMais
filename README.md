assistente-alzheimer/
├── README.md                     # Visão geral, instruções de execução e membros da equipe
├── docs/
│   ├── prd.md                    # Product Requirements Document (personas, RNs, FAs, escopo)
│   └── arquitetura.md            # Diagrama de classes POO, camadas e fluxo de dados
├── specs/
│   ├── spec-lembretes.md         # Especificação detalhada dos lembretes inteligentes e FA01
│   ├── spec-geolocalizacao.md    # Especificação de zona segura e alertas de deslocamento
│   └── spec-monitoramento.md     # Especificação de monitoramento comportamental e dashboard
├── database/
│   └── schema.sql                # Script DDL completo + dados iniciais (seed) para testes
└── src/
    └── br/edu/si/alzheimer/
        ├── model/                # Usuario, Cuidador, Paciente, Lembrete, ZonaSegura, RegistroComportamental
        ├── service/              # LembreteService, GeolocalizacaoService, MonitoramentoService
        ├── repository/           # Interfaces e implementações JDBC/JPA
        └── Main.java             # Ponto de entrada e simulação dos fluxos
