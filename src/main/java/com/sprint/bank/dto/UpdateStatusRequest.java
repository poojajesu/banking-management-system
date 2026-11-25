package com.sprint.bank.dto;

import com.sprint.bank.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "Account status is required (ACTIVE, FROZEN, or CLOSED)")
    private AccountStatus status;

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
