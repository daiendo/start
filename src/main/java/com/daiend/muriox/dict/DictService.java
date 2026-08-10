package com.daiend.muriox.dict;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class DictService {

    private final DictMapper dictMapper;

    public DictService(DictMapper dictMapper) {
        this.dictMapper = dictMapper;
    }

    public Optional<String> findLabel(
            String dictCode,
            String value) {

        if (dictCode == null || dictCode.isBlank()) {
            return Optional.empty();
        }

        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                dictMapper.selectLabel(dictCode, value));
    }
}