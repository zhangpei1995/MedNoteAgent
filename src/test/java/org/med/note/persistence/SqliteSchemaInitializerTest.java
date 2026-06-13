package org.med.note.persistence;

import org.junit.jupiter.api.Test;
import org.med.note.MedNoteAgentApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MedNoteAgentApplication.class)
class SqliteSchemaInitializerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allBusinessTablesShouldUseAutoIncrementIdPrimaryKey() {
        for (String tableName : List.of(
                "agent_runs",
                "agent_steps",
                "agent_tool_calls",
                "knowledge_graph_nodes",
                "knowledge_graph_edges"
        )) {
            Map<String, Object> idColumn = column(tableName, "id");

            assertEquals("INTEGER", String.valueOf(idColumn.get("type")).toUpperCase());
            assertEquals(1, ((Number) idColumn.get("pk")).intValue());
        }
    }

    @Test
    void relationshipTablesShouldReferenceAutoIncrementIds() {
        assertColumnExists("agent_steps", "agent_run_id");
        assertColumnExists("agent_tool_calls", "agent_run_id");
        assertColumnExists("knowledge_graph_edges", "source_node_row_id");
        assertColumnExists("knowledge_graph_edges", "target_node_row_id");

        assertColumnAbsent("agent_steps", "session_id");
        assertColumnAbsent("agent_tool_calls", "session_id");
        assertColumnAbsent("knowledge_graph_edges", "source_node_id");
        assertColumnAbsent("knowledge_graph_edges", "target_node_id");
        assertColumnAbsent("knowledge_graph_edges", "edge_id");
    }

    private void assertColumnExists(String tableName, String columnName) {
        column(tableName, columnName);
    }

    private void assertColumnAbsent(String tableName, String columnName) {
        assertTrue(columns(tableName).stream().noneMatch(column -> columnName.equals(column.get("name"))));
    }

    private Map<String, Object> column(String tableName, String columnName) {
        return columns(tableName).stream()
                .filter(column -> columnName.equals(column.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少字段: " + tableName + "." + columnName));
    }

    private List<Map<String, Object>> columns(String tableName) {
        return jdbcTemplate.queryForList("PRAGMA table_info(" + tableName + ")");
    }
}
