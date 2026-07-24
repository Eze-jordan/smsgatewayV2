package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository chargé de gérer l'accès aux données des documents.
 *
 * JpaRepository fournit automatiquement les opérations CRUD principales :
 * - enregistrer un document ;
 * - rechercher un document par son identifiant ;
 * - récupérer tous les documents ;
 * - modifier un document ;
 * - supprimer un document.
 *
 * L'entité gérée est {@link Document} et son identifiant principal
 * est de type {@link Long}.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Recherche un document à partir de son nom de fichier d'origine.
     *
     * Le nom d'origine correspond généralement au nom du fichier
     * envoyé par l'utilisateur avant son éventuel renommage
     * ou son stockage sur le serveur.
     *
     * Exemple :
     * facture-juillet-2026.pdf
     *
     * Optional est utilisé car aucun document ne peut correspondre
     * au nom fourni.
     *
     * @param originalName nom d'origine du fichier à rechercher
     * @return document trouvé dans un Optional, ou Optional.empty()
     *         si aucun document ne correspond
     */
    Optional<Document> findByOriginalName(String originalName);

    /**
     * Supprime le ou les documents correspondant au nom de fichier
     * d'origine fourni.
     *
     * Attention : si plusieurs documents possèdent le même nom d'origine,
     * ils peuvent tous être supprimés selon la structure de la base.
     *
     * Cette méthode supprime uniquement l'enregistrement en base de données.
     * Elle ne supprime pas automatiquement le fichier physique stocké
     * sur le disque, sauf si cette suppression est gérée ailleurs
     * dans le service applicatif.
     *
     * Il est recommandé d'appeler cette méthode depuis une méthode
     * annotée avec {@code @Transactional}.
     *
     * @param originalName nom d'origine du fichier à supprimer
     */
    void deleteByOriginalName(String originalName);
}