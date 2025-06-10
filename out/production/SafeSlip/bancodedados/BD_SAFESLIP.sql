CREATE DATABASE SAFESLIP;


USE SAFESLIP;

CREATE TABLE Usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE CNPJ_Emitente (
    cnpj VARCHAR(14) PRIMARY KEY,
    nome_razao_social VARCHAR(150),
    data_abertura DATE,
    situacao_cadastral VARCHAR(50),
    atualizado_em DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE CNPJ_Reputacao (
    cnpj VARCHAR(14) PRIMARY KEY,
    score_reputacao DECIMAL(5,2) NOT NULL,
    total_boletos INT DEFAULT 0,
    total_denuncias INT DEFAULT 0,
    ultima_atualizacao DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cnpj) REFERENCES CNPJ_Emitente(cnpj)
);

CREATE TABLE Boleto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    codigo_barras VARCHAR(100) UNIQUE NOT NULL,
    cnpj_emitente VARCHAR(14) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    vencimento DATE NOT NULL,
    data_extracao DATETIME DEFAULT CURRENT_TIMESTAMP,
    status_validacao ENUM('VALIDO', 'INVALIDO', 'ERRO') NOT NULL,
	nome_beneficiario VARCHAR(150),
    banco_emissor VARCHAR(100),
	denunciado BOOLEAN DEFAULT FALSE,
	nome_cnpj_receita VARCHAR(150),
    FOREIGN KEY (cnpj_emitente) REFERENCES CNPJ_Emitente(cnpj)
);

CREATE TABLE Denuncia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descricao TEXT NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    boleto_id INT,
    data_denuncia DATETIME DEFAULT CURRENT_TIMESTAMP,
    usuario_id INT,
    FOREIGN KEY (boleto_id) REFERENCES Boleto(id),
    FOREIGN KEY (usuario_id) REFERENCES Usuario(id)
);

CREATE TABLE Log_Verificacao (
    id INT AUTO_INCREMENT PRIMARY KEY,
    boleto_id INT,
    resultado TEXT NOT NULL,
    data_verificacao DATETIME DEFAULT CURRENT_TIMESTAMP,
    reputacao_cnpj DECIMAL(5,2),
    mensagens TEXT,
    FOREIGN KEY (boleto_id) REFERENCES Boleto(id)
);

CREATE TABLE PrevisaoFraude (
    id INT AUTO_INCREMENT PRIMARY KEY,
    boleto_id INT,
    probabilidade_fraude DECIMAL(5,2), 
    data_execucao DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (boleto_id) REFERENCES Boleto(id)
);

SELECT * FROM Denuncia;

CREATE VIEW VisaoAnaliseBoleto AS
SELECT
    b.cnpj_emitente AS CNPJ,
    b.nome_beneficiario AS Nome,
    b.valor AS Valor,
    b.banco_emissor AS Banco,
    b.vencimento AS `Data Venc.`,
    b.denunciado AS `Já denunciado`,
    cr.total_denuncias AS Denúncias,
    -- diferença do nome real pro nome do boleto (pode ser heurística)
    CASE 
        WHEN b.nome_cnpj_receita IS NOT NULL AND b.nome_beneficiario IS NOT NULL THEN
            1 - (LENGTH(b.nome_beneficiario) - LENGTH(REPLACE(b.nome_beneficiario, SUBSTRING_INDEX(b.nome_cnpj_receita, ' ', 1), ''))) / LENGTH(b.nome_beneficiario)
        ELSE NULL
    END AS `Nome bate?`,
    CASE 
        WHEN ce.situacao_cadastral = 'ATIVA' THEN true
        ELSE false
    END AS `CNPJ válido`,
    cr.score_reputacao AS Reputacao,
    CASE 
        WHEN pf.probabilidade_fraude >= 70 THEN true
        ELSE false
    END AS `É fraude?`
FROM
    Boleto b
LEFT JOIN CNPJ_Emitente ce ON b.cnpj_emitente = ce.cnpj
LEFT JOIN CNPJ_Reputacao cr ON b.cnpj_emitente = cr.cnpj
LEFT JOIN PrevisaoFraude pf ON b.id = pf.boleto_id;

INSERT INTO Usuario (nome, email) VALUES
('Luara Lima', 'luara@example.com'),
('Gabriel Costa', 'gabriel@example.com'),
('Ana Martins', 'ana@example.com');


INSERT INTO CNPJ_Emitente (cnpj, nome_razao_social, data_abertura, situacao_cadastral)
VALUES
('12345678000195', 'Empresa Fictícia A LTDA', '2010-01-10', 'ATIVA'),
('98765432000111', 'Tech Corp B S.A.', '2015-07-23', 'ATIVA'),
('11223344000100', 'Loja Exemplo C ME', '2020-03-15', 'INATIVA');

INSERT INTO Boleto (codigo_barras, cnpj_emitente, valor, vencimento, status_validacao, nome_beneficiario, banco_emissor, denunciado, nome_cnpj_receita)
VALUES
('23793381286000000012345678901234567890123456', '12345678000195', 350.75, '2025-07-10', 'VALIDO', 'Empresa Fictícia A LTDA', 'Bradesco', TRUE, 'Empresa Fictícia A LTDA'),
('00193373700000015001234567890123456789012345', '98765432000111', 150.00, '2025-07-15', 'VALIDO', 'Tech Corp B S.A.', 'Banco do Brasil', FALSE, 'Tech Corp B S.A.'),
('34191090080000001231234567890123456789012345', '11223344000100', 75.90, '2025-07-20', 'INVALIDO', 'Loja Exemplo C', 'Itaú', TRUE, 'Loja Exemplo C ME');

INSERT INTO Denuncia (descricao, cnpj, boleto_id, usuario_id)
VALUES
('Boleto falso enviado por e-mail.', '12345678000195', 1, 1),
('Valor diferente do informado pelo atendente.', '11223344000100', 3, 2);

INSERT INTO CNPJ_Reputacao (cnpj, score_reputacao, total_boletos, total_denuncias)
VALUES
('12345678000195', 0.80, 1, 1),
('98765432000111', 1.00, 1, 0),
('11223344000100', 0.50, 1, 1);

INSERT INTO PrevisaoFraude (boleto_id, probabilidade_fraude)
VALUES
(1, 85.00),
(2, 20.00),
(3, 95.00);

SELECT * FROM VisaoAnaliseBoleto;

ALTER TABLE CNPJ_Reputacao MODIFY score_reputacao DECIMAL(5,2) NOT NULL;

