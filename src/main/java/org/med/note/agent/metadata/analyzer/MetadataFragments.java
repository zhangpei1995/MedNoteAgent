package org.med.note.agent.metadata.analyzer;

import lombok.Data;
import org.med.note.domain.metadata.ConsultationCategory;
import org.med.note.domain.metadata.KnowledgeStatus;
import org.med.note.domain.metadata.ScopeStatus;
import org.med.note.domain.metadata.SessionMetadataFragment;
import org.med.note.domain.metadata.SessionMetadataResult;

/**
 * 元数据分析项输出片段。
 */
public final class MetadataFragments {

    private MetadataFragments() {
    }

    @Data
    public static class Title implements SessionMetadataFragment {

        private String title;

        @Override
        public void applyTo(SessionMetadataResult result) {
            result.setTitle(title);
        }
    }

    @Data
    public static class ConsultationCategoryValue implements SessionMetadataFragment {

        private ConsultationCategory consultationCategory;

        @Override
        public void applyTo(SessionMetadataResult result) {
            result.setConsultationCategory(consultationCategory);
        }
    }

    @Data
    public static class RetrievalTarget implements SessionMetadataFragment {

        private String recognizedDrugName;

        private String instructionItem;

        private KnowledgeStatus knowledgeStatus;

        private String understandingText;

        @Override
        public void applyTo(SessionMetadataResult result) {
            result.setRecognizedDrugName(recognizedDrugName);
            result.setInstructionItem(instructionItem);
            result.setKnowledgeStatus(knowledgeStatus);
            result.setUnderstandingText(understandingText);
        }
    }

    @Data
    public static class ScopeBoundary implements SessionMetadataFragment {

        private ScopeStatus scopeStatus;

        @Override
        public void applyTo(SessionMetadataResult result) {
            result.setScopeStatus(scopeStatus);
        }
    }
}
