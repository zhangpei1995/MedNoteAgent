import { Dispatch, SetStateAction, useCallback, useEffect, useRef } from 'react';
import { Message } from '@arco-design/web-react';
import { chatApi } from '../api/chatApi';
import type { ChatSessionSummary, ChatSessionTitleResponse, ChatSessionTitleStatus } from '../types/chat';

const TITLE_POLL_INTERVAL_MS = 1200;
const MAX_TITLE_POLL_ATTEMPTS = 25;
const TERMINAL_TITLE_STATUSES = new Set<ChatSessionTitleStatus>(['GENERATED', 'FAILED']);

interface UseSessionTitleSyncOptions {
  sessions: ChatSessionSummary[];
  setSessions: Dispatch<SetStateAction<ChatSessionSummary[]>>;
}

interface TitlePollingEntry {
  timerId?: number;
}

function isTerminalTitleStatus(status: ChatSessionTitleStatus): boolean {
  return TERMINAL_TITLE_STATUSES.has(status);
}

function mergeSessionTitle(
  sessions: ChatSessionSummary[],
  titleResponse: ChatSessionTitleResponse,
): ChatSessionSummary[] {
  return sessions.map((session) => {
    if (session.sessionId !== titleResponse.sessionId) {
      return session;
    }

    return {
      ...session,
      title: titleResponse.title,
      titleStatus: titleResponse.titleStatus,
      titleGeneratedAt: titleResponse.titleGeneratedAt,
    };
  });
}

/**
 * 统一同步会话标题生成状态。
 *
 * <p>标题由后端异步生成；该 hook 以 sessionId 为维度为所有 GENERATING 会话持续拉取标题接口，
 * 并把结果合并回会话列表，让多会话并行提问时的顶部标题和侧栏历史项共享同一份标题状态。</p>
 */
export function useSessionTitleSync({
  sessions,
  setSessions,
}: UseSessionTitleSyncOptions) {
  const pollingEntriesRef = useRef<Map<string, TitlePollingEntry>>(new Map());

  const stopSessionTitleSync = useCallback((sessionId: string) => {
    const pollingEntry = pollingEntriesRef.current.get(sessionId);
    if (pollingEntry?.timerId) {
      window.clearTimeout(pollingEntry.timerId);
    }
    pollingEntriesRef.current.delete(sessionId);
  }, []);

  const stopAllSessionTitleSync = useCallback(() => {
    pollingEntriesRef.current.forEach((pollingEntry) => {
      if (pollingEntry.timerId) {
        window.clearTimeout(pollingEntry.timerId);
      }
    });
    pollingEntriesRef.current.clear();
  }, []);

  const pollSessionTitle = useCallback(
    async (sessionId: string, attempt = 0) => {
      if (!pollingEntriesRef.current.has(sessionId)) {
        pollingEntriesRef.current.set(sessionId, {});
      }

      try {
        const titleResponse = await chatApi.getSessionTitle(sessionId);
        setSessions((currentSessions) => mergeSessionTitle(currentSessions, titleResponse));

        if (isTerminalTitleStatus(titleResponse.titleStatus) || attempt >= MAX_TITLE_POLL_ATTEMPTS) {
          stopSessionTitleSync(sessionId);
          return;
        }

        const timerId = window.setTimeout(() => {
          void pollSessionTitle(sessionId, attempt + 1);
        }, TITLE_POLL_INTERVAL_MS);
        pollingEntriesRef.current.set(sessionId, { timerId });
      } catch (error) {
        stopSessionTitleSync(sessionId);
        Message.error('会话标题加载失败');
      }
    },
    [setSessions, stopSessionTitleSync],
  );

  const syncSessionTitle = useCallback(
    (sessionId: string) => {
      if (pollingEntriesRef.current.has(sessionId)) {
        return;
      }

      pollingEntriesRef.current.set(sessionId, {});
      void pollSessionTitle(sessionId);
    },
    [pollSessionTitle],
  );

  useEffect(() => {
    sessions
      .filter((session) => session.titleStatus === 'GENERATING')
      .forEach((session) => syncSessionTitle(session.sessionId));
  }, [sessions, syncSessionTitle]);

  useEffect(() => stopAllSessionTitleSync, [stopAllSessionTitleSync]);

  return {
    syncSessionTitle,
  };
}
