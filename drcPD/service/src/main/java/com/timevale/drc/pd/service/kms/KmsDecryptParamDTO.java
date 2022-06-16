package com.timevale.drc.pd.service.kms;

import lombok.Data;

@Data
public class KmsDecryptParamDTO {

    private String keyId;
    private String encryptedContent;

    public KmsDecryptParamDTO(String keyId, String encryptedContent) {
        this.keyId = keyId;
        this.encryptedContent = encryptedContent;
    }
}
