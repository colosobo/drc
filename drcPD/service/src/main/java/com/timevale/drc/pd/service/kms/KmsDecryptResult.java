package com.timevale.drc.pd.service.kms;

import lombok.Data;

@Data
public class KmsDecryptResult {

    private boolean success;

    private String message;

    private String keyId;

    private String decryptContent;

    private String requestId;
}
