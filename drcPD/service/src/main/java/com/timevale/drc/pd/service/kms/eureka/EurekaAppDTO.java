package com.timevale.drc.pd.service.kms.eureka;

import lombok.Data;

import java.util.List;

@Data
public class EurekaAppDTO {

    private String name;
    private List<EurekaInstanceDTO> instance;
}
