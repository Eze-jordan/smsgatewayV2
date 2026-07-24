package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.model.SmsRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository chargé de gérer l'accès aux destinataires des SMS.
 *
 * Chaque enregistrement SmsRecipient représente généralement
 * un destinataire associé à un SMS, avec son propre statut d'envoi.
 *
 * JpaRepository fournit automatiquement les opérations CRUD principales :
 * - enregistrer un destinataire ;
 * - rechercher un destinataire par son identifiant ;
 * - récupérer les destinataires ;
 * - modifier un destinataire ;
 * - supprimer un destinataire.
 *
 * L'entité gérée est {@link SmsRecipient} et son identifiant principal
 * est de type {@link Long}.
 */
public interface SmsRecipientRepository
        extends JpaRepository<SmsRecipient, Long> {

    /**
     * Recherche tous les destinataires associés à une référence SMS
     * et possédant un statut précis.
     *
     * Exemple :
     * récupérer tous les destinataires en échec pour un SMS donné.
     *
     * @param ref référence du SMS
     * @param statut statut des destinataires recherchés
     * @return liste des destinataires correspondant aux critères
     */
    List<SmsRecipient> findBySms_RefAndStatut(
            String ref,
            SmsStatus statut
    );

    /**
     * Recherche, avec pagination, les destinataires associés
     * à une référence SMS et à un statut précis.
     *
     * Cette version est adaptée aux SMS contenant un grand nombre
     * de destinataires.
     *
     * @param ref référence du SMS
     * @param statut statut des destinataires recherchés
     * @param pageable paramètres de pagination et de tri
     * @return page contenant les destinataires correspondant aux critères
     */
    Page<SmsRecipient> findBySms_RefAndStatut(
            String ref,
            SmsStatus statut,
            Pageable pageable
    );

    /**
     * Vérifie si au moins un destinataire existe pour une référence SMS
     * et un statut précis.
     *
     * Cette méthode est plus efficace qu'une récupération complète
     * lorsque seule l'existence d'un résultat doit être contrôlée.
     *
     * @param ref référence du SMS
     * @param statut statut recherché
     * @return true si au moins un destinataire correspond, sinon false
     */
    boolean existsBySms_RefAndStatut(
            String ref,
            SmsStatus statut
    );

    /**
     * Recherche tous les destinataires associés à plusieurs références SMS.
     *
     * Cette méthode peut être utilisée pour charger les destinataires
     * de plusieurs campagnes ou messages en une seule requête.
     *
     * @param refs liste des références SMS recherchées
     * @return liste des destinataires associés aux références
     */
    List<SmsRecipient> findBySms_RefIn(
            List<String> refs
    );

    /**
     * Supprime tous les destinataires associés à une référence SMS.
     *
     * Cette opération doit de préférence être appelée depuis une méthode
     * de service annotée avec {@code @Transactional}.
     *
     * @param ref référence du SMS dont les destinataires doivent être supprimés
     */
    void deleteBySms_Ref(
            String ref
    );

    /**
     * Supprime tous les destinataires associés à plusieurs références SMS.
     *
     * Cette méthode est utile lors de la suppression groupée
     * de plusieurs SMS ou campagnes.
     *
     * Cette opération doit de préférence être appelée depuis une méthode
     * de service annotée avec {@code @Transactional}.
     *
     * @param refs liste des références SMS concernées
     */
    void deleteBySms_RefIn(
            List<String> refs
    );
}