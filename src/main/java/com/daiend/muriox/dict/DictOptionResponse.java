package com.daiend.muriox.dict;

public record DictOptionResponse(
        Long id,
        String label,
        Object value,
        String tagType
) {
}
