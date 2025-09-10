package com.ogooueTech.smsgateway.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record CreateContactRequest(
        @NotBlank
        @JsonAlias({"group_id", "id_clients_groups", "id_group", "group"})
        String groupId,
        @NotBlank String number,
        String name
) {}
