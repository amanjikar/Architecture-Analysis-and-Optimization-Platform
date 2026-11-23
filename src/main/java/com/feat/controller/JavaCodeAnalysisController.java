
package com.feat.controller;

import org.springframework.web.bind.annotation.*;

import com.feat.dto.CodeAnalysisResponse;
import com.feat.service.JavaCodeAnalysisService;

@RestController
@RequestMapping("/api/projects")
public class JavaCodeAnalysisController {

    private final JavaCodeAnalysisService analysisService;

    public JavaCodeAnalysisController(
            JavaCodeAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    // Analyse imported Java project
    @PostMapping("/{id}/analyze")
    public CodeAnalysisResponse analyzeProject(
            @PathVariable Long id) {

        return analysisService.analyzeProject(id);
    }
}
