# PRD - Assistente de Rotina para Pessoas com Alzheimer

## 1. Visão Geral
Aplicativo voltado para apoiar pacientes com Alzheimer e seus familiares/cuidadores na gestão diária da rotina, segurança espacial e acompanhamento comportamental.

## 2. Personas
- **Paciente (ex.: Carlos):** Interface simplificada em alto contraste (WCAG AAA), comandos diretos (`JÁ FIZ` / `ADIAR`) e estímulos auditivos com voz familiar.
- **Cuidador Familiar (ex.: João):** Configura rotinas, recebe alertas críticos em tempo real e acompanha relatórios de adesão e comportamento.

## 3. Regras de Negócio
- **RN01 (Acessibilidade WCAG AAA):** Telas do paciente com alto contraste e ações binárias claras.
- **RN02 (Voz Familiar):** Lembretes priorizam gravações de áudio de familiares próximos.
- **FA01 (Escalonamento por Inatividade):** Após 15 minutos sem resposta, o alarme aumenta gradativamente e envia Alerta Vermelho ao cuidador.
- **RN03 (Limite de Adiamentos):** Máximo de 2 adiamentos por lembrete antes de escalonar ao cuidador.
- **RN04 (Cerca Virtual):** Saída do raio definido na `ZonaSegura` dispara alerta imediato.
