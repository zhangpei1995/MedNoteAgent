import { Dispatch, SetStateAction, useCallback, useEffect, useRef } from 'react';
import { Message } from '@arco-design/web-react';
import { chatApi } from '../api/chatApi';
import type { ChatSessionMetadataResponse, ChatSessionMetadataStatus, ChatSessionSummary } from '../types/chat';

const METADATA_POLL_INTERVAL_MS = 1200;
const MAX_METADATA_POLL_ATTEMPTS = 25;
const TERMINAL_METADATA_STATUSES = new Set<ChatSessionMetadataStatus>(['GENERATED', 'FAILED']);

interface UseSessionMetadataSyncOptions {
  sessions: ChatSessionSummary[];
  setSessions: Dispatch<SetStateAction<ChatSessionSummary[]>>;
}

interface MetadataPollingEntry {
  timerId?: number;
}

function isTerminalMetadataStatus(status: ChatSessionMetadataStatus): boolean {
  return TERMINAL_METADATA_STATUSES.has(status);
}

function mergeSessionMetadata(
  sessions: ChatSessionSummary[],
  metadata: ChatSessionMetadataResponse,
): ChatSessionSummary[] {
  return sessions.map((session) => {
    if (session.sessionId !== metadata.sessionId) {
      return session;
    }

    return {
      ...session,
      title: metadata.title,
      metadataStatus: metadata.metadataStatus,
      consultationCategory: metadata.consultationCategory,
      consultationCategoryLabel: metadata.consultationCategoryLabel,
      recognizedDrugName: metadata.recognizedDrugName,
      instructionItem: metadata.instructionItem,
      knowledgeStatus: metadata.knowledgeStatus,
      knowledgeStatusLabel: metadata.knowledgeStatusLabel,
      scopeStatus: metadata.scopeStatus,
      scopeStatusLabel: metadata.scopeStatusLabel,
      understandingText: metadata.understandingText,
      metadataGeneratedAt: metadata.generatedAt,
    };
  });
}

/**
 * 同步会话元数据异步生成状态。
 */
export function useSessionMetadataSync({ sessions, setSessions }: UseSessionMetadataSyncOptions) {
  const pollingEntriesRef = useRef<Map<string, MetadataPollingEntry>>(new Map());

  const stopSessionMetadataSync = useCallback((sessionId: string) => {
    const pollingEntry = pollingEntriesRef.current.get(sessionId);
    if (pollingEntry?.timerId) {
      window.clearTimeout(pollingEntry.timerId);
    }
    pollingEntriesRef.current.delete(sessionId);
  }, []);

  const stopAllSessionMetadataSync = useCallback(() => {
    pollingEntriesRef.current.forEach((pollingEntry) => {
      if (pollingEntry.timerId) {
        window.clearTimeout(pollingEntry.timerId);
      }
    });
    pollingEntriesRef.current.clear();
  }, []);

  const pollSessionMetadata = useCallback(
    async (sessionId: string, attempt = 0) => {
      if (!pollingEntriesRef.current.has(sessionId)) {
        pollingEntriesRef.current.set(sessionId, {});
      }

      try {
        const metadata = await chatApi.getSessionMetadata(sessionId);
        setSessions((currentSessions) => mergeSessionMetadata(currentSessions, metadata));

        if (isTerminalMetadataStatus(metadata.metadataStatus) || attempt >= MAX_METADATA_POLL_ATTEMPTS) {
          stopSessionMetadataSync(sessionId);
          return;
        }

        const timerId = window.setTimeout(() => {
          void pollSessionMetadata(sessionId, attempt + 1);
        }, METADATA_POLL_INTERVAL_MS);
        pollingEntriesRef.current.set(sessionId, { timerId });
      } catch (error) {
        stopSessionMetadataSync(sessionId);
        Message.error('会话元数据加载失败');
      }
    },
    [setSessions, stopSessionMetadataSync],
  );

  const syncSessionMetadata = useCallback(
    (sessionId: string) => {
      if (pollingEntriesRef.current.has(sessionId)) {
        return;
      }

      pollingEntriesRef.current.set(sessionId, {});
      void pollSessionMetadata(sessionId);
    },
    [pollSessionMetadata],
  );

  useEffect(() => {
    sessions
      .filter((session) => session.metadataStatus === 'GENERATING')
      .forEach((session) => syncSessionMetadata(session.sessionId));
  }, [sessions, syncSessionMetadata]);

  useEffect(() => stopAllSessionMetadataSync, [stopAllSessionMetadataSync]);

  return {
    syncSessionMetadata,
  };
}
