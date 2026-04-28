package pt.luis.projectaboutanimals.model;

public enum DonationStatus {
    RASCUNHO,          // produtos: antes de submeter
    SUBMETIDA,         // produtos submetidos
    PAYPAL_CRIADA,     // dinheiro: criada
    PAYPAL_APROVADA,
    EM_TRIAGEM,
    RECEBIDA,// dinheiro: aprovada (redirect)
    PAGA,              // dinheiro: capturada
    CANCELADA,
    FALHADA
}
