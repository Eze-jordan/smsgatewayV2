package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.enums.TypeCompte;
import com.ogooueTech.smsgateway.model.Client;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository chargé de gérer l'accès aux données des clients.
 *
 * JpaRepository fournit automatiquement les principales opérations CRUD :
 * - enregistrer un client ;
 * - rechercher un client par son identifiant ;
 * - récupérer tous les clients ;
 * - modifier un client ;
 * - supprimer un client.
 *
 * L'entité gérée est {@link Client} et son identifiant principal
 * est de type {@link String}.
 */
public interface ClientRepository extends JpaRepository<Client, String> {

    /**
     * Vérifie si un client possède déjà l'adresse e-mail indiquée.
     *
     * Cette méthode peut notamment être utilisée avant la création
     * d'un nouveau client afin d'éviter les doublons.
     *
     * @param email adresse e-mail à vérifier
     * @return true si l'adresse e-mail existe déjà, sinon false
     */
    boolean existsByEmail(String email);

    /**
     * Vérifie si une clé API est déjà attribuée à un client.
     *
     * Cette vérification permet de garantir qu'une même clé API
     * n'est pas utilisée par plusieurs clients.
     *
     * @param cleApi clé API à vérifier
     * @return true si la clé API existe déjà, sinon false
     */
    boolean existsByCleApi(String cleApi);

    /**
     * Recherche un client à partir de son adresse e-mail.
     *
     * @param email adresse e-mail du client
     * @return client trouvé dans un Optional, ou Optional.empty()
     *         si aucun client ne correspond
     */
    Optional<Client> findByEmail(String email);

    /**
     * Recherche un client à partir de sa clé API.
     *
     * Cette méthode peut être utilisée pour authentifier ou identifier
     * un client lors d'un appel vers l'API.
     *
     * @param cleApi clé API du client
     * @return client trouvé dans un Optional, ou Optional.empty()
     *         si aucune clé ne correspond
     */
    Optional<Client> findByCleApi(String cleApi);

    /**
     * Recherche un client à partir de la propriété idclients.
     *
     * Cette méthode est utile lorsque le nom du champ de l'entité
     * est explicitement défini comme "idclients".
     *
     * @param idclients identifiant du client
     * @return client trouvé dans un Optional, ou Optional.empty()
     *         si aucun client ne correspond
     */
    Optional<Client> findByIdclients(String idclients);

    /**
     * Recherche un client par son identifiant tout en appliquant
     * un verrou pessimiste en écriture.
     *
     * Le verrou PESSIMISTIC_WRITE empêche une autre transaction
     * de modifier simultanément le même client jusqu'à la fin
     * de la transaction en cours.
     *
     * Cette méthode est particulièrement utile pour les opérations sensibles,
     * comme la modification d'un solde, d'un quota ou d'un compteur.
     *
     * Cette méthode doit être appelée depuis une méthode annotée
     * avec {@code @Transactional}.
     *
     * @param id identifiant du client à verrouiller
     * @return client verrouillé dans un Optional, ou Optional.empty()
     *         si aucun client ne correspond
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Client c where c.idclients = :id")
    Optional<Client> lockById(@Param("id") String id);

    /**
     * Recherche les clients correspondant à un type de compte
     * et à un statut de compte précis.
     *
     * Exemple :
     * récupérer tous les comptes professionnels actuellement actifs.
     *
     * @param typeCompte type de compte recherché
     * @param statutCompte statut du compte recherché
     * @return liste des clients correspondant aux deux critères
     */
    List<Client> findByTypeCompteAndStatutCompte(
            TypeCompte typeCompte,
            StatutCompte statutCompte
    );

    /**
     * Recherche les clients dont la raison sociale contient
     * le texte indiqué, sans tenir compte des majuscules et minuscules.
     *
     * Exemple :
     * une recherche avec "ogooue" peut trouver "Ogooue Tech".
     *
     * @param raisonSociale texte à rechercher dans la raison sociale
     * @return liste des clients dont la raison sociale correspond
     */
    List<Client> findByRaisonSocialeContainingIgnoreCase(
            String raisonSociale
    );
}