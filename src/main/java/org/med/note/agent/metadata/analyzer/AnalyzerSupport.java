package org.med.note.agent.metadata.analyzer;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 元数据分析器共享的轻量解析能力。
 */
final class AnalyzerSupport {

    private AnalyzerSupport() {
    }

    static JSONObject parseObject(String rawOutput) {
        String text = StrUtil.trimToEmpty(rawOutput);
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("模型未返回 JSON 对象");
        }
        return JSONUtil.parseObj(text.substring(start, end + 1));
    }

    static <E extends Enum<E>> E enumOrDefault(Class<E> enumType, String value, E defaultValue) {
        String code = StrUtil.trimToEmpty(value);
        if (StrUtil.isBlank(code)) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, code);
        } catch (IllegalArgumentException exception) {
            return defaultValue;
        }
    }
}
