CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil ENUM('CUIDADOR', 'PACIENTE') NOT NULL,
    cuidador_id INT NULL,
    extra VARCHAR(100),
    CONSTRAINT fk_paciente_cuidador FOREIGN KEY (cuidador_id) REFERENCES usuario(id)
);

CREATE TABLE lembrete (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    horario_programado TIME NOT NULL,
    caminho_audio_voz VARCHAR(255),
    status ENUM('PENDENTE', 'ADIADO', 'CONFIRMADO', 'ESCALONADO') NOT NULL DEFAULT 'PENDENTE',
    confirmado_em DATETIME NULL,
    cuidador_id INT NOT NULL,
    paciente_id INT NOT NULL,
    CONSTRAINT fk_lembrete_cuidador FOREIGN KEY (cuidador_id) REFERENCES usuario(id),
    CONSTRAINT fk_lembrete_paciente FOREIGN KEY (paciente_id) REFERENCES usuario(id)
);

CREATE TABLE zona_segura (
    id INT AUTO_INCREMENT PRIMARY KEY,
    paciente_id INT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    latitude_centro DECIMAL(10, 7) NOT NULL,
    longitude_centro DECIMAL(10, 7) NOT NULL,
    raio_metros DECIMAL(8, 2) NOT NULL,
    CONSTRAINT fk_zona_paciente FOREIGN KEY (paciente_id) REFERENCES usuario(id)
);

CREATE TABLE registro_localizacao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    paciente_id INT NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    fora_zona_segura BOOLEAN NOT NULL DEFAULT FALSE,
    registrado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loc_paciente FOREIGN KEY (paciente_id) REFERENCES usuario(id)
);

CREATE TABLE registro_comportamental (
    id INT AUTO_INCREMENT PRIMARY KEY,
    paciente_id INT NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    intensidade INT NOT NULL CHECK (intensidade BETWEEN 1 AND 5),
    observacao TEXT,
    registrado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comp_paciente FOREIGN KEY (paciente_id) REFERENCES usuario(id)
);
