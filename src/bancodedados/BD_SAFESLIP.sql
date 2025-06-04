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
    score_reputacao DECIMAL(3,2) NOT NULL,
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
