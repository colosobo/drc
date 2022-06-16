package com.timevale.drc.pd.deploy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author gwk_2
 * @date 2022/1/18 11:54
 */
@RestController
@RequestMapping("/")
public class Health {

    @GetMapping("/health")
    public String health() {
        return "\"status\":\"UP\"";
    }
}
