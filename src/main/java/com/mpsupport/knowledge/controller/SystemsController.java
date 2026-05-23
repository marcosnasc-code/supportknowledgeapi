package com.mpsupport.knowledge.controller;

import com.mpsupport.knowledge.dto.SystemCatalogItem;
import com.mpsupport.knowledge.service.SystemHintService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/systems")
public class SystemsController {

    private final SystemHintService systemHintService;

    public SystemsController(SystemHintService systemHintService) {
        this.systemHintService = systemHintService;
    }

    @GetMapping
    public List<SystemCatalogItem> listSystems() {
        return systemHintService.listCatalog();
    }
}
