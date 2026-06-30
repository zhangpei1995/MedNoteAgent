import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Message } from '@arco-design/web-react';
import { chatApi } from '../api/chatApi';
import { useSessionTitleSync } from './useSessionTitleSync';
import type {
  ChatSessionSummary,
  ChatTurnRecord,
  ChatTurnStatus,
  ChatTurnStatusResponse,
  SubmitChatTurnResponse,
} from '../types/chat';

const SELECTED_SESSION_STORAGE_KEY = 'med-note-agent:selected-session-id';
const LOCAL_USER_ID = 'local-user';
const TERMINAL_STATUSES = new Set<ChatTurnStatus>(['SUCCESS', 'FAILED']);
const POLL_INTERVAL_MS = 1400;
const MAX_POLL_ATTEMPTS = 90;
const SESSION_SEARCH_DEBOUNCE_MS = 300;

function isTerminalStatus(status: ChatTurnStatus): boolean {
  return TERMINAL_STATUSES.has(status);
}

function asTurnRecord(statusResponse: ChatTurnStatusResponse): ChatTurnRecord {
  return {
    turnId: statusResponse.turnId,
    sessionId: statusResponse.sessionId,
    userInput: statusResponse.userInput,
    assistantOutput: statusResponse.assistantOutput,
    status: statusResponse.status,
    errorMessage: statusResponse.errorMessage,
    createdAt: statusResponse.createdAt,
    completedAt: statusResponse.completedAt,
  };
}

function mergeTurn(turns: ChatTurnRecord[], nextTurn: ChatTurnRecord): ChatTurnRecord[] {
  const existingIndex = turns.findIndex((turn) => turn.turnId === nextTurn.turnId);
  if (existingIndex < 0) {
    return [...turns, nextTurn].sort((left, right) => left.createdAt.localeCompare(right.createdAt));
  }

  return turns.map((turn, index) => (index === existingIndex ? nextTurn : turn));
}

function upsertSubmittedSession(
  sessions: ChatSessionSummary[],
  response: SubmitChatTurnResponse,
): ChatSessionSummary[] {
  const submittedSession: ChatSessionSummary = {
    sessionId: response.sessionId,
    userId: response.userId,
    title: response.title,
    titleStatus: response.titleStatus,
    titleGeneratedAt: response.titleGeneratedAt,
    status: response.status,
    createdAt: response.sessionCreatedAt,
    updatedAt: response.sessionUpdatedAt,
    endedAt: response.endedAt,
  };

  return [
    submittedSession,
    ...sessions.filter((session) => session.sessionId !== response.sessionId),
  ];
}

function readStoredSessionId(): string | undefined {
  return localStorage.getItem(SELECTED_SESSION_STORAGE_KEY) ?? undefined;
}

export function useChatWorkspace() {
  const [sessions, setSessions] = useState<ChatSessionSummary[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState<string | undefined>(readStoredSessionId);
  const [sessionKeyword, setSessionKeyword] = useState('');
  const [turns, setTurns] = useState<ChatTurnRecord[]>([]);
  const [sessionLoading, setSessionLoading] = useState(false);
  const [turnsLoading, setTurnsLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [pollingTurnId, setPollingTurnId] = useState<string | undefined>();
  const selectedSessionIdRef = useRef<string | undefined>(selectedSessionId);
  const pollingTimerRef = useRef<number | undefined>();

  const selectedSession = useMemo(
    () => sessions.find((session) => session.sessionId === selectedSessionId),
    [selectedSessionId, sessions],
  );
  const selectedSessionTitle = useMemo(() => {
    if (selectedSession?.title) {
      return selectedSession.title;
    }

    return turns.find((turn) => turn.sessionId === selectedSessionId)?.userInput;
  }, [selectedSession, selectedSessionId, turns]);
  const hasActiveSession = Boolean(selectedSessionId);

  const { syncSessionTitle } = useSessionTitleSync({
    sessions,
    setSessions,
  });

  const refreshSessions = useCallback(async (keyword = sessionKeyword) => {
    setSessionLoading(true);
    try {
      const normalizedKeyword = keyword.trim();
      const nextSessions = await chatApi.listSessions(normalizedKeyword || undefined);
      setSessions(nextSessions);
    } catch (error) {
      Message.error('会话列表加载失败，请确认后端服务已启动');
    } finally {
      setSessionLoading(false);
    }
  }, [sessionKeyword]);

  const loadTurns = useCallback(async (sessionId: string | undefined) => {
    if (!sessionId) {
      setTurns([]);
      return;
    }

    setTurnsLoading(true);
    try {
      const nextTurns = await chatApi.listSessionTurns(sessionId);
      setTurns(nextTurns);
    } catch (error) {
      Message.error('会话轮次加载失败');
      setTurns([]);
    } finally {
      setTurnsLoading(false);
    }
  }, []);

  const selectSession = useCallback((sessionId: string) => {
    selectedSessionIdRef.current = sessionId;
    setSelectedSessionId(sessionId);
  }, []);

  const startNewSession = useCallback(() => {
    selectedSessionIdRef.current = undefined;
    setSelectedSessionId(undefined);
    setTurns([]);
  }, []);

  const searchSessions = useCallback((keyword: string) => {
    setSessionKeyword(keyword);
  }, []);

  const pollTurnStatus = useCallback(
    async (turnId: string, attempt = 0) => {
      setPollingTurnId(turnId);
      try {
        const statusResponse = await chatApi.getTurnStatus(turnId);
        const nextTurn = asTurnRecord(statusResponse);
        setTurns((currentTurns) => mergeTurn(currentTurns, nextTurn));

        if (isTerminalStatus(statusResponse.status) || attempt >= MAX_POLL_ATTEMPTS) {
          setPollingTurnId(undefined);
          await refreshSessions();
          return;
        }

        pollingTimerRef.current = window.setTimeout(() => {
          void pollTurnStatus(turnId, attempt + 1);
        }, POLL_INTERVAL_MS);
      } catch (error) {
        setPollingTurnId(undefined);
        Message.error('轮次状态查询失败');
      }
    },
    [refreshSessions],
  );

  const sendMessage = useCallback(
    async (userInput: string) => {
      const trimmedInput = userInput.trim();
      if (!trimmedInput || sending) {
        return;
      }

      setSending(true);
      try {
        const activeSessionId = selectedSessionIdRef.current;
        const response = await chatApi.submitTurn({
          sessionId: activeSessionId,
          userId: LOCAL_USER_ID,
          userInput: trimmedInput,
        });

        setSessions((currentSessions) => upsertSubmittedSession(currentSessions, response));
        selectedSessionIdRef.current = response.sessionId;
        setSelectedSessionId(response.sessionId);
        setTurns((currentTurns) =>
          mergeTurn(currentTurns, {
            turnId: response.turnId,
            sessionId: response.sessionId,
            userInput: trimmedInput,
            assistantOutput: null,
            status: response.turnStatus,
            errorMessage: null,
            createdAt: response.createdAt,
            completedAt: null,
          }),
        );
        await refreshSessions();
        if (!activeSessionId && response.titleStatus === 'GENERATING') {
          syncSessionTitle(response.sessionId);
        }
        void pollTurnStatus(response.turnId);
      } catch (error) {
        Message.error('消息发送失败，请确认后端服务可用');
      } finally {
        setSending(false);
      }
    },
    [pollTurnStatus, refreshSessions, sending, syncSessionTitle],
  );

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void refreshSessions(sessionKeyword);
    }, SESSION_SEARCH_DEBOUNCE_MS);

    return () => {
      window.clearTimeout(timer);
    };
  }, [refreshSessions, sessionKeyword]);

  useEffect(() => {
    selectedSessionIdRef.current = selectedSessionId;
    void loadTurns(selectedSessionId);
    if (selectedSessionId) {
      localStorage.setItem(SELECTED_SESSION_STORAGE_KEY, selectedSessionId);
    } else {
      localStorage.removeItem(SELECTED_SESSION_STORAGE_KEY);
    }
  }, [loadTurns, selectedSessionId]);

  useEffect(() => {
    return () => {
      if (pollingTimerRef.current) {
        window.clearTimeout(pollingTimerRef.current);
      }
    };
  }, []);

  return {
    sessions,
    selectedSession,
    selectedSessionTitle,
    hasActiveSession,
    selectedSessionId,
    turns,
    sessionLoading,
    sessionKeyword,
    turnsLoading,
    sending,
    pollingTurnId,
    refreshSessions,
    searchSessions,
    selectSession,
    startNewSession,
    sendMessage,
  };
}
