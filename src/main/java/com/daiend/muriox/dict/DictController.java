package com.daiend.muriox.dict;

import com.daiend.muriox.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/authority/dict")
public class DictController {
    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/option/{code}")
    public ApiResponse<List<DictOptionResponse>> options(
            @PathVariable String code) {

        return ApiResponse.ok(
                dictService.findOptions(code));
    }
}
