package org.med.note.service.impl;

import org.med.note.service.spi.RequestPlanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Rule-based request understanding service for the local agent pipeline.
 */
@Service
public class RuleBasedRequestPlanner implements RequestPlanner {

    private final int maxKeywords;

    public RuleBasedRequestPlanner(@Value("${mednote.agent.keyword.max-keywords:12}") int maxKeywords) {
        this.maxKeywords = maxKeywords;
    }

    @Override
    public RequestPlanner.Plan plan(String topic, String input) {
        String text = (topic == null ? "" : topic) + " " + (input == null ? "" : input);
        String intent = recognizeIntent(text);
        List<String> queryTargets = queryTargets(text, intent);
        List<String> riskSignals = medicationRiskSignals(text);
        String riskLevel = medicationRiskLevel(intent, riskSignals);
        List<String> instructions = recommendedInstructions(text, queryTargets, riskSignals);
        List<String> taskKeywords = taskKeywords(text, queryTargets, riskSignals, instructions);
        String rewrittenQuery = rewriteQuery(topic, input, intent, taskKeywords);
        return new RequestPlanner.Plan(
                intent,
                taskKeywords,
                rewrittenQuery,
                queryKeywords(taskKeywords, rewrittenQuery),
                queryTargets,
                riskLevel,
                riskSignals,
                instructions
        );
    }

    private List<String> taskKeywords(String text, List<String> queryTargets, List<String> riskSignals, List<String> instructions) {
        Set<String> keywords = new LinkedHashSet<>();
        for (String candidate : List.of(
                "二冬汤颗粒", "菖麻熄风颗粒", "功能主治", "适应症", "用法用量", "禁忌", "过敏", "过敏体质",
                "不良反应", "副作用", "注意事项", "孕妇", "儿童", "老人", "肝肾功能", "合并用药", "风险", "证据", "摘要", "回答"
        )) {
            if (text.contains(candidate)) {
                keywords.add(candidate);
            }
        }
        keywords.addAll(queryTargets);
        keywords.addAll(riskSignals);
        keywords.addAll(instructions);
        for (String token : text.split("[\\s，。；、：,.?？!！]+")) {
            if (token.length() >= 2 && token.length() <= 12) {
                keywords.add(token);
            }
            if (keywords.size() >= maxKeywords) {
                break;
            }
        }
        return new ArrayList<>(keywords).stream().limit(Math.max(1, maxKeywords)).toList();
    }

    private String recognizeIntent(String text) {
        if (containsAny(text, "禁忌", "过敏", "不能吃", "能不能吃", "是否能服用")) {
            return "CONTRAINDICATION";
        }
        if (containsAny(text, "不良反应", "副作用", "恶心", "胃部不适")) {
            return "ADVERSE_REACTION";
        }
        if (containsAny(text, "用法", "用量", "怎么吃", "服用", "一次", "一日")) {
            return "DOSAGE_ADVICE";
        }
        if (containsAny(text, "孕妇", "儿童", "老人", "肝肾", "运动员")) {
            return "SPECIAL_POPULATION";
        }
        if (containsAny(text, "注意事项", "注意", "忌口", "烟", "酒", "辛辣")) {
            return "CAUTION";
        }
        if (containsAny(text, "说明书", "推荐", "哪个药", "什么药")) {
            return "INSTRUCTION_RECOMMENDATION";
        }
        return "GENERAL_QA";
    }

    private List<String> queryTargets(String text, String intent) {
        Set<String> targets = new LinkedHashSet<>();
        if (containsAny(text, "功能主治", "适应症", "症状", "咳", "咽")) {
            targets.add("功能主治");
        }
        if (containsAny(text, "用法", "用量", "怎么吃", "服用")) {
            targets.add("用法用量");
        }
        if (containsAny(text, "禁忌", "过敏", "不能吃", "能不能吃")) {
            targets.add("禁忌");
        }
        if (containsAny(text, "注意", "忌口", "烟", "酒", "辛辣")) {
            targets.add("注意事项");
        }
        if (containsAny(text, "不良反应", "副作用", "恶心", "胃部不适")) {
            targets.add("不良反应");
        }
        if (targets.isEmpty()) {
            targets.add(switch (intent) {
                case "DOSAGE_ADVICE" -> "用法用量";
                case "CONTRAINDICATION" -> "禁忌";
                case "ADVERSE_REACTION" -> "不良反应";
                case "CAUTION", "SPECIAL_POPULATION" -> "注意事项";
                default -> "功能主治";
            });
        }
        return new ArrayList<>(targets);
    }

    private List<String> medicationRiskSignals(String text) {
        Set<String> signals = new LinkedHashSet<>();
        for (String signal : List.of("禁忌", "过敏", "过敏体质", "孕妇", "儿童", "老人", "肝肾功能", "合并用药", "不良反应", "副作用", "运动员")) {
            if (text.contains(signal)) {
                signals.add(signal);
            }
        }
        if (containsAny(text, "不能吃", "能不能吃", "是否能服用")) {
            signals.add("服用可行性");
        }
        return new ArrayList<>(signals);
    }

    private String medicationRiskLevel(String intent, List<String> signals) {
        if (List.of("CONTRAINDICATION", "SPECIAL_POPULATION", "ADVERSE_REACTION").contains(intent)) {
            return "HIGH";
        }
        if (!signals.isEmpty() || "DOSAGE_ADVICE".equals(intent) || "CAUTION".equals(intent)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private List<String> recommendedInstructions(String text, List<String> queryTargets, List<String> riskSignals) {
        Set<String> instructions = new LinkedHashSet<>();
        if (text.contains("二冬汤") || containsAny(text, "咽干", "干咳", "咽痛", "养阴润肺")) {
            instructions.add("二冬汤颗粒说明书");
        }
        if (text.contains("菖麻熄风") || containsAny(text, "平肝熄风", "化痰通络", "风痰阻络", "运动员", "肝肾功能", "合并用药")) {
            instructions.add("菖麻熄风颗粒说明书");
        }
        if (instructions.isEmpty() && (queryTargets.contains("禁忌") || riskSignals.contains("过敏"))) {
            instructions.add("菖麻熄风颗粒说明书");
        }
        if (instructions.isEmpty()) {
            instructions.add("二冬汤颗粒说明书");
            instructions.add("菖麻熄风颗粒说明书");
        }
        return new ArrayList<>(instructions);
    }

    private String rewriteQuery(String topic, String input, String intent, List<String> taskKeywords) {
        return (topic + " " + input + " " + String.join(" ", taskKeywords) + " " + switch (intent) {
            case "CONTRAINDICATION" -> "禁忌 过敏 慎用";
            case "ADVERSE_REACTION" -> "不良反应 副作用 停药";
            case "DOSAGE_ADVICE" -> "用法用量 开水冲服 一日";
            case "SPECIAL_POPULATION" -> "儿童 孕妇 老人 肝肾功能";
            case "CAUTION" -> "注意事项 忌口 就医";
            default -> "功能主治 注意事项 用法用量";
        }).trim();
    }

    private List<String> queryKeywords(List<String> taskKeywords, String rewrittenQuery) {
        Set<String> keywords = new LinkedHashSet<>(taskKeywords == null ? List.of() : taskKeywords);
        for (String token : rewrittenQuery.split("[\\s，。；、：,.?？!！]+")) {
            if (token.length() >= 2 && token.length() <= 12) {
                keywords.add(token);
            }
            if (keywords.size() >= 16) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

}
