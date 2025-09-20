package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.ClientsContacts;
import io.jsonwebtoken.io.IOException;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

public interface ClientsContactsService {

    ClientsContacts addToGroup(String groupId, String number, String name);

    List<ClientsContacts> listByGroup(String groupId);

    ClientsContacts get(String contactId);

    ClientsContacts patch(String contactId, String number, String name);

    void delete(String contactId);
    // ✅ nouveaux :
    ClientsContacts move(String contactId, String targetGroupId);

    ImportReportxls importCsvToGroup(String groupId, InputStream csvStream, Charset charset);

    List<ClientsContacts> search(String query);
    List<ClientsContacts> listByClient(String clientId);

    List<ClientsContacts> searchByClient(String clientId, String query);


    // petit POJO de rapport
    class ImportReport {
        public final String groupId;
        public final int totalRows;
        public final int inserted;
        public final int duplicates;
        public final List<String> duplicateNumbers;

        public ImportReport(String groupId, int totalRows, int inserted, int duplicates, List<String> duplicateNumbers) {
            this.groupId = groupId; this.totalRows = totalRows; this.inserted = inserted;
            this.duplicates = duplicates; this.duplicateNumbers = duplicateNumbers;
        }
    }
    List<ClientsContacts> listAll();

    // ⬇️ Dé-commente si tu veux gérer aussi les XLSX (avec Apache POI)
    ImportReport importXlsxToGroup(String groupId, InputStream xlsxStream) throws IOException, java.io.IOException;

    /* ---------- DTO de résultat d'import ---------- */
    record ImportReportxls(
            String groupId,
            int total,           // lignes lues (hors entête)
            int inserted,        // insérées
            int duplicates,      // doublons / invalides
            List<String> duplicateNumbers // numéros en doublon / invalides (pour feedback)
    ) {}

}
