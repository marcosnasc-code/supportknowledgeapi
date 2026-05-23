package com.mpsupport.knowledge.controller;

import com.mpsupport.knowledge.dto.AssistRequest;
import com.mpsupport.knowledge.dto.AssistResponse;
import com.mpsupport.knowledge.service.AssistService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assist")
public class AssistController {

    private final AssistService assistService;

    public AssistController(AssistService assistService) {
        this.assistService = assistService;
    }

    @PostMapping
    public AssistResponse assist(@Valid @RequestBody AssistRequest request) {
        return assistService.assist(request);
    }
}
