package org.med.note.agent.runtime;

import org.med.note.agent.tool.AgentToolDescriptor;
import org.med.note.agent.tool.ToolContext;
import org.med.note.agent.tool.ToolResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes selected agent tools and converts each invocation into an audit record.
 */
@Component
public class AgentToolExecutor {

    private final ExecutorService toolExecutor;
    private final AgentContextMerger contextMerger;

    public AgentToolExecutor(
            AgentContextMerger contextMerger,
            @Value("${mednote.agent.executor.parallelism:4}") int parallelism
    ) {
        this.contextMerger = contextMerger;
        this.toolExecutor = Executors.newFixedThreadPool(Math.max(1, parallelism));
    }

    public List<ToolExecutionResult> executeBatch(String sessionId, int firstToolOrder, ToolSelectionBatch batch, ToolContext context) {
        ToolContext batchContext = contextMerger.snapshot(context);
        List<CompletableFuture<ToolExecutionResult>> futures = new ArrayList<>();
        for (int index = 0; index < batch.selectedTools().size(); index++) {
            ToolSelectionBatch.SelectedTool selectedTool = batch.selectedTools().get(index);
            int toolOrder = firstToolOrder + index;
            futures.add(CompletableFuture.supplyAsync(
                    () -> executeTool(sessionId, toolOrder, selectedTool, batchContext),
                    toolExecutor
            ));
        }
        return futures.stream()
                .map(CompletableFuture::join)
                .sorted(Comparator.comparingInt(execution -> execution.record().order()))
                .toList();
    }

    @PreDestroy
    public void shutdown() {
        toolExecutor.shutdown();
    }

    private ToolExecutionResult executeTool(String sessionId, int order, ToolSelectionBatch.SelectedTool selection, ToolContext context) {
        Instant startedAt = Instant.now();
        Map<String, Object> inputSnapshot = inputSnapshot(context);
        AgentToolDescriptor descriptor = selection.descriptor();
        try {
            ToolResult result = selection.tool().execute(context);
            Instant finishedAt = Instant.now();
            ToolCallRecord record = ToolCallRecord.completed(
                    sessionId,
                    order,
                    descriptor.name(),
                    descriptor.phase(),
                    startedAt,
                    finishedAt,
                    result.summary(),
                    inputSnapshot,
                    result.metadata()
            );
            return new ToolExecutionResult(record, result);
        } catch (Exception error) {
            Instant finishedAt = Instant.now();
            ToolCallRecord record = ToolCallRecord.failed(
                    sessionId,
                    order,
                    descriptor.name(),
                    descriptor.phase(),
                    startedAt,
                    finishedAt,
                    inputSnapshot,
                    error
            );
            return new ToolExecutionResult(record, null);
        }
    }

    private Map<String, Object> inputSnapshot(ToolContext context) {
        return Map.of(
                "intent", context.intent(),
                "taskKeywords", context.taskKeywords(),
                "queryKeywords", context.queryKeywords(),
                "evidenceCount", context.evidence().size(),
                "riskLevel", context.riskLevel()
        );
    }
}
