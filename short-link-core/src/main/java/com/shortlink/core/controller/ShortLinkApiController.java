package com.shortlink.core.controller;

import com.shortlink.common.result.Result;
import com.shortlink.core.service.ShortLinkGenerateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortLinkApiController {

    @Autowired
    private ShortLinkGenerateService generateService;

    @PostMapping("/short/generate")
    public Result<String> generate(@RequestParam String url) {
        String shortCode = generateService.generateShortLink(url);
        return Result.success(shortCode);
    }
}
