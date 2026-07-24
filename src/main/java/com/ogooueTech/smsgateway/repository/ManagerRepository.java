package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository chargé de gérer l'accès aux données des managers.
 *
 * JpaRepository fournit automatiquement les opérations CRUD principales :
 * - créer ou enregistrer un manager ;
 * - rechercher un manager par son identifiant ;
 * - récupérer tous les managers ;
 * - modifier un manager ;
 * - supprimer un manager.
 *
 * L'entité gérée est {@link Manager} et son identifiant principal
 * est de type {@link String}.
 */
public interface ManagerRepository extends JpaRepository<Manager, String> {

    /**
     * Recherche un manager à partir de son adresse e-mail.
     *
     * Cette méthode peut notamment être utilisée pendant
     * l'authentification ou l'affichage du profil d'un manager.
     *
     * @param email adresse e-mail du manager recherché
     * @return manager trouvé dans un Optional, ou Optional.empty()
     *         si aucun manager ne correspond
     */
    Optional<Manager> findByEmail(
            String email
    );

    /**
     * Vérifie si une adresse e-mail est déjà utilisée par un manager.
     *
     * Cette méthode peut être appelée avant la création ou la modification
     * d'un compte afin d'éviter les doublons.
     *
     * @param email adresse e-mail à vérifier
     * @return true si l'adresse e-mail existe déjà, sinon false
     */
    boolean existsByEmail(
            String email
    );

    /**
     * Vérifie si un numéro de téléphone est déjà utilisé
     * par un manager.
     *
     * Le nom "NumeroTelephoneManager" doit correspondre exactement
     * à l'attribut déclaré dans l'entité Manager.
     *
     * @param numeroTelephoneManager numéro de téléphone à vérifier
     * @return true si le numéro existe déjà, sinon false
     */
    boolean existsByNumeroTelephoneManager(
            String numeroTelephoneManager
    );
}