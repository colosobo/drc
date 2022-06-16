package com.timevale.drc.pd.service.kms;

import lombok.Data;

@Data
public class KmsValueDTO {

    private String kmsKeyId;

    private String cipherText;
}
