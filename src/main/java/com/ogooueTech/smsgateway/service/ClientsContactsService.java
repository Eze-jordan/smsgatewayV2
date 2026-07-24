package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.ClientsContacts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Contrat de service chargé de gérer les contacts des clients.
 *
 * Il permet notamment :
 * - d'ajouter un contact dans un groupe ;
 * - de modifier ou supprimer un contact ;
 * - de déplacer un contact vers un autre groupe ;
 * - d'importer des contacts depuis un fichier CSV ou XLSX ;
 * - de rechercher les contacts d'un client.
 */
public interface ClientsContactsService {

    /**
     * Ajoute un contact dans un groupe.
     *
     * @param groupId identifiant du groupe
     * @param number numéro de téléphone du contact
     * @param name nom du contact
     * @return contact créé
     */
    ClientsContacts addToGroup(
            String groupId,
            String number,
            String name
    );

    /**
     * Liste tous les contacts appartenant à un groupe.
     *
     * @param groupId identifiant du groupe
     * @return liste des contacts du groupe
     */
    List<ClientsContacts> listByGroup(String groupId);

    /**
     * Recherche un contact par son identifiant.
     *
     * @param contactId identifiant du contact
     * @return contact correspondant
     */
    ClientsContacts get(String contactId);

    /**
     * Modifie partiellement un contact.
     *
     * Les valeurs nulles peuvent être ignorées par l'implémentation.
     *
     * @param contactId identifiant du contact
     * @param number nouveau numéro de téléphone
     * @param name nouveau nom du contact
     * @return contact après modification
     */
    ClientsContacts patch(
            String contactId,
            String number,
            String name
    );

    /**
     * Supprime un contact.
     *
     * @param contactId identifiant du contact
     */
    void delete(String contactId);

    /**
     * Déplace un contact vers un autre groupe.
     *
     * @param contactId identifiant du contact
     * @param targetGroupId identifiant du groupe de destination
     * @return contact après déplacement
     */
    ClientsContacts move(
            String contactId,
            String targetGroupId
    );

    /**
     * Importe des contacts depuis un fichier CSV.
     *
     * Le flux doit être fermé par la couche qui l'a ouvert,
     * généralement le contrôleur ou le gestionnaire du fichier envoyé.
     *
     * @param groupId identifiant du groupe de destination
     * @param csvStream contenu du fichier CSV
     * @param charset encodage du fichier
     * @return rapport détaillé de l'importation
     */
    ImportReportxls importCsvToGroup(
            String groupId,
            InputStream csvStream,
            Charset charset
    );

    /**
     * Recherche des contacts, tous clients confondus.
     *
     * Cette méthode doit être réservée aux administrateurs.
     *
     * @param query texte recherché dans le nom ou le numéro
     * @return liste des contacts correspondants
     */
    List<ClientsContacts> search(String query);

    /**
     * Liste les contacts appartenant à un client.
     *
     * @param clientId identifiant du client
     * @return liste des contacts du client
     */
    List<ClientsContacts> listByClient(String clientId);

    /**
     * Recherche les contacts appartenant à un client précis.
     *
     * @param clientId identifiant du client
     * @param query texte recherché dans le nom ou le numéro
     * @return liste des contacts correspondants
     */
    List<ClientsContacts> searchByClient(
            String clientId,
            String query
    );

    /**
     * Liste tous les contacts, tous clients confondus.
     *
     * Cette méthode doit être protégée par une autorisation
     * réservée aux administrateurs.
     *
     * @return liste complète des contacts
     */
    List<ClientsContacts> listAll();

    /**
     * Importe des contacts depuis un fichier Excel XLSX.
     *
     * L'implémentation nécessite généralement Apache POI.
     *
     * @param groupId identifiant du groupe de destination
     * @param xlsxStream contenu du fichier XLSX
     * @return rapport de l'importation
     * @throws IOException si le fichier ne peut pas être lu
     */
    ImportReport importXlsxToGroup(
            String groupId,
            InputStream xlsxStream
    ) throws IOException;

    /**
     * Rapport utilisé pour l'importation XLSX.
     */
    final class ImportReport {

        public final String groupId;
        public final int totalRows;
        public final int inserted;
        public final int duplicates;
        public final List<String> duplicateNumbers;

        public ImportReport(
                String groupId,
                int totalRows,
                int inserted,
                int duplicates,
                List<String> duplicateNumbers
        ) {
            this.groupId = groupId;
            this.totalRows = totalRows;
            this.inserted = inserted;
            this.duplicates = duplicates;
            this.duplicateNumbers = duplicateNumbers == null
                    ? List.of()
                    : List.copyOf(duplicateNumbers);
        }
    }

    /**
     * Rapport utilisé pour l'importation CSV.
     *
     * @param groupId identifiant du groupe de destination
     * @param total nombre de lignes lues hors en-tête
     * @param inserted nombre de contacts insérés
     * @param duplicates nombre de doublons ou de lignes invalides
     * @param duplicateNumbers numéros en doublon ou invalides
     */
    record ImportReportxls(
            String groupId,
            int total,
            int inserted,
            int duplicates,
            List<String> duplicateNumbers
    ) {

        /**
         * Constructeur compact permettant de protéger la liste
         * contre les modifications extérieures.
         */
        public ImportReportxls {
            duplicateNumbers = duplicateNumbers == null
                    ? List.of()
                    : List.copyOf(duplicateNumbers);
        }
    }
}