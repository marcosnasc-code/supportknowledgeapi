package com.mpsupport.knowledge.controller;

import com.mpsupport.knowledge.dto.IndexStatusResponse;
import com.mpsupport.knowledge.service.IndexStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/index")
public class IndexController {

    private final IndexStatusService indexStatusService;

    public IndexController(IndexStatusService indexStatusService) {
        this.indexStatusService = indexStatusService;
    }

    @GetMapping("/status")
    public IndexStatusResponse status() {
        return indexStatusService.getStatus();
    }
}
