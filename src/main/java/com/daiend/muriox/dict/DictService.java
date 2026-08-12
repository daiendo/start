package com.daiend.muriox.dict;

import java.util.List;
import java.util.Optional;

import com.daiend.muriox.common.exception.BusinessException;
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

    public List<DictOptionResponse> findOptions(String code) {
        if (code == null || code.isBlank()) {
            return List.of();
        }

        return dictMapper.selectOptionsByCode(code)
                .stream()
                .map(row -> new DictOptionResponse(
                        row.getId(),
                        row.getLabel(),
                        convertValue(row.getValueType(), row.getValue()),
                        row.getTagType()))
                .toList();
    }

    private Object convertValue(
            DictValueType valueType,
            String value) {

        if (valueType == null) {
            throw new BusinessException("字典值类型不能为空");
        }

        return switch (valueType) {
            case STRING -> value;

            case INTEGER -> {
                try {
                    yield Long.valueOf(value);
                } catch (NumberFormatException exception) {
                    throw new BusinessException(
                            "字典值不是有效整数：" + value);
                }
            }

            case BOOLEAN -> {
                if (!"true".equalsIgnoreCase(value)
                        && !"false".equalsIgnoreCase(value)) {
                    throw new BusinessException(
                            "字典值不是有效布尔值：" + value);
                }

                yield Boolean.valueOf(value);
            }
        };
    }

}