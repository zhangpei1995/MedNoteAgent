package org.med.note.agent.metadata;

import org.med.note.domain.metadata.SessionMetadataFragment;
import org.med.note.domain.metadata.SessionMetadataResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 会话元数据分析编排器。
 *
 * <p>编排器按分析项顺序执行可用 Analyzer，并把各分析项片段合并为最终元数据结果。</p>
 */
@Component
public class SessionMetadataGenerationOrchestrator {

    private final SessionMetadataModelClient modelClient;
    private final List<SessionMetadataAnalyzer<?>> analyzers;

    public SessionMetadataGenerationOrchestrator(
            SessionMetadataModelClient modelClient,
            List<SessionMetadataAnalyzer<?>> analyzers
    ) {
        this.modelClient = modelClient;
        this.analyzers = analyzers.stream()
                .sorted(Comparator.comparingInt(SessionMetadataAnalyzer::order))
                .toList();
    }

    /**
     * 执行会话元数据分析。
     *
     * @param context 会话分析上下文
     * @return 合并后的元数据结果
     */
    public SessionMetadataResult generate(SessionMetadataContext context) {
        SessionMetadataResult result = new SessionMetadataResult();
        for (SessionMetadataAnalyzer<?> analyzer : analyzers) {
            if (!analyzer.supports(context, result)) {
                continue;
            }

            String rawOutput = modelClient.call(analyzer.buildMessages(context, result));
            SessionMetadataFragment fragment = analyzer.parse(rawOutput, context, result);
            fragment.applyTo(result);
        }
        return result;
    }
}
