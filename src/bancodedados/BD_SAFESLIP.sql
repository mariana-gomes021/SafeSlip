-- Garante que o banco de dados existe e o seleciona
DROP DATABASE IF EXISTS SAFESLIP;
CREATE DATABASE IF NOT EXISTS SAFESLIP;

USE SAFESLIP;

-- Criação da tabela Usuario
CREATE TABLE IF NOT EXISTS Usuario (
    id INT AUTO_INCREMENT PRIMARY KEY
);

-- Criação da tabela CNPJ_Emitente
CREATE TABLE IF NOT EXISTS CNPJ_Emitente (
    cnpj VARCHAR(14) PRIMARY KEY,
    nome_razao_social VARCHAR(150),
    data_abertura DATE,
    atualizado_em DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Criação da tabela Boleto
CREATE TABLE IF NOT EXISTS Boleto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    codigo_barras VARCHAR(100) UNIQUE NOT NULL,
    cnpj_emitente VARCHAR(14) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    vencimento DATE NOT NULL,
    data_extracao DATETIME DEFAULT CURRENT_TIMESTAMP,
    status_validacao VARCHAR(50) NOT NULL, -- Alterado para VARCHAR para mais flexibilidade de status
    nome_beneficiario VARCHAR(150),
    banco_emissor VARCHAR(100),
    denunciado BOOLEAN DEFAULT FALSE,
    nome_cnpj_receita VARCHAR(150), -- Mantido, mas pode ser redundante com razaoSocialApi
    usuario_id INT, -- Adicionado para ligar ao usuário
    informacoes_confirmadas_pelo_usuario BOOLEAN DEFAULT FALSE, -- Adicionado para confirmar dados
    -- Adiciona as colunas dos ALTERS diretamente na criação da tabela
    status_validacao_banco VARCHAR(50),
    nome_banco_api VARCHAR(150),
    nome_completo_banco_api VARCHAR(255),
    ispb_banco_api VARCHAR(50),
    razao_social_api VARCHAR(150),
    nome_fantasia_api VARCHAR(150),
    FOREIGN KEY (cnpj_emitente) REFERENCES CNPJ_Emitente(cnpj)
);

-- Adiciona a chave estrangeira para 'usuario_id' na tabela Boleto
-- Esta Foreign Key é adicionada após a criação da tabela Usuario para garantir que a referência exista.
ALTER TABLE Boleto
ADD CONSTRAINT fk_usuario
FOREIGN KEY (usuario_id) REFERENCES Usuario(id);

-- Criação da tabela CNPJ_Reputacao
CREATE TABLE IF NOT EXISTS CNPJ_Reputacao (
    cnpj VARCHAR(14) PRIMARY KEY,
    score_reputacao DECIMAL(5,2) NOT NULL DEFAULT 100.00, -- Score em porcentagem, inicializado em 100%
    total_boletos INT DEFAULT 0,
    total_denuncias INT DEFAULT 0,
    ultima_atualizacao DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cnpj) REFERENCES CNPJ_Emitente(cnpj)
);


USE SAFESLIP;

-- Criação ou substituição da visão VisaoAnaliseBoleto
DROP VIEW IF EXISTS VisaoAnaliseBoleto; -- Garante que a visão antiga seja removida antes de recriar

CREATE OR REPLACE VIEW VisaoAnaliseBoleto AS
SELECT
    b.id AS boleto_id,
    b.codigo_barras AS CodigoBarras,
    b.cnpj_emitente AS CNPJ,
    -- Usando a Razão Social da API como o principal nome do emitente para análise
    COALESCE(b.razao_social_api, ce.nome_razao_social, 'Não disponível') AS RazaoSocialEmitente,
    b.valor AS Valor,
    b.vencimento AS `Data Venc.`,
    b.banco_emissor AS BancoExtraidoPDF, -- Nome do banco extraído do PDF
    b.nome_banco_api AS BancoAPI, -- Nome do banco validado/extraído pela API
    b.status_validacao AS StatusValidacaoGeral,
    b.status_validacao_banco AS StatusValidacaoBanco,
    -- Informações Confirmadas Pelo Usuário (true/false, sem 1/0)
    CASE b.informacoes_confirmadas_pelo_usuario
        WHEN TRUE THEN 'Sim'
        WHEN FALSE THEN 'Não'
        ELSE 'Indefinido'
    END AS ConfirmadoPeloUsuario,
    cr.total_boletos AS TotalBoletosCNPJ,
    cr.total_denuncias AS TotalDenunciasCNPJ,
    cr.score_reputacao AS ReputacaoCNPJScore,
    CASE
        WHEN cr.score_reputacao > 80 THEN 'Confiável'
        WHEN cr.score_reputacao >= 50 AND cr.score_reputacao <= 80 THEN 'Risco Moderado'
        WHEN cr.score_reputacao < 50 AND cr.score_reputacao > 0 THEN 'Problemático'
        WHEN cr.score_reputacao = 0 THEN 'Reincidente'
        ELSE 'Não Classificado'
    END AS ClassificacaoCNPJ,
    -- Nova lógica para 'Nome bate?'
    CASE
        WHEN b.nome_beneficiario IS NULL OR b.nome_beneficiario = '' THEN 'Sem nome no PDF'
        WHEN b.razao_social_api IS NULL OR b.razao_social_api = '' THEN 'Sem Razão Social da API'
        -- Compara se o nome do PDF está contido na Razão Social da API ou vice-versa, de forma insensível a maiúsculas/minúsculas e espaços
        WHEN REPLACE(LOWER(b.nome_beneficiario), ' ', '') LIKE CONCAT('%', REPLACE(LOWER(b.razao_social_api), ' ', ''), '%') THEN 'Sim'
        WHEN REPLACE(LOWER(b.razao_social_api), ' ', '') LIKE CONCAT('%', REPLACE(LOWER(b.nome_beneficiario), ' ', ''), '%') THEN 'Sim'
        ELSE 'Não'
    END AS NomeBate,
    -- Indicador se o boleto foi marcado como denunciado automaticamente
    CASE b.denunciado
        WHEN TRUE THEN 'Sim'
        WHEN FALSE THEN 'Não'
        ELSE 'Indefinido'
    END AS DenunciadoAutomaticamente
FROM
    Boleto b
LEFT JOIN CNPJ_Emitente ce ON b.cnpj_emitente = ce.cnpj
LEFT JOIN CNPJ_Reputacao cr ON b.cnpj_emitente = cr.cnpj;

-- Exemplos de consultas (mantidos do seu script)
SELECT * FROM VisaoAnaliseBoleto;

SELECT
    id,
    cnpj_emitente,
    nome_beneficiario,
    razao_social_api,
    status_validacao
FROM Boleto
ORDER BY id DESC
LIMIT 10;

SELECT * FROM Boleto;
select * from CNPJ_Emitente;
ALTER TABLE Boleto DROP COLUMN nome_cnpj_receita;

