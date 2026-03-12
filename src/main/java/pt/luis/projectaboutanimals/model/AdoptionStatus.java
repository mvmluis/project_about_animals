package pt.luis.projectaboutanimals.model;

public enum AdoptionStatus {
    PENDENTE,
    EM_ANALISE,
    APROVADO,

    VISITA_MARCADA,
    VISITA_REALIZADA,

    // passos do processo
    MICROCHIP,          // Identificação (microchip) — mantém-se igual se preferires
    DESPARASITACAO,
    ESTERILIZACAO,
    PRONTO,

    CONTRATO_PENDENTE,
    ADOTADO,

    CANCELADO,
    REJEITADO
}
