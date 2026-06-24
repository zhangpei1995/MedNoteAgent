export type ChatTurnStatus = 'WAITING_AGENT' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | string;

export interface ChatSessionSummary {
  sessionId: string;
  userId?: string | null;
  title: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  endedAt?: string | null;
}

export interface ChatTurnRecord {
  turnId: string;
  sessionId: string;
  userInput: string;
  assistantOutput?: string | null;
  status: ChatTurnStatus;
  errorMessage?: string | null;
  createdAt: string;
  completedAt?: string | null;
}

export interface SubmitChatTurnRequest {
  sessionId?: string;
  userId?: string;
  title?: string;
  userInput: string;
}

export interface SubmitChatTurnResponse {
  sessionId: string;
  turnId: string;
  title: string;
  turnStatus: ChatTurnStatus;
  createdAt: string;
}

export interface ChatTurnStatusResponse {
  turnId: string;
  sessionId: string;
  title: string;
  status: ChatTurnStatus;
  userInput: string;
  assistantOutput?: string | null;
  errorMessage?: string | null;
  createdAt: string;
  completedAt?: string | null;
}
