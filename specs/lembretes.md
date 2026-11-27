# Spec: Lembretes Inteligentes
- Cadastro de lembrete com horário (`LocalTime`), título e caminho de áudio familiar.
- Estados possíveis (`StatusLembrete`): `PENDENTE`, `ADIADO`, `CONFIRMADO`, `ESCALONADO`.
- Fluxo de confirmação atualiza status e notifica cuidador.
- Fluxo de timeout (`>= 15 min`) dispara alerta vermelho.
