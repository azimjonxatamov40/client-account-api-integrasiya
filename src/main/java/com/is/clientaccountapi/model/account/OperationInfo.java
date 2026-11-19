package com.is.clientaccountapi.model.account;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OperationInfo {
    private Long id;
    private Long accountId;
    private Long amount;
    private String purpose;
    private String createdAt;
}
