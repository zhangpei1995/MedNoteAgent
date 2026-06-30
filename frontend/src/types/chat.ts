export type ChatTurnStatus = 'WAITING_AGENT' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | string;
export type ChatSessionTitleStatus = 'GENERATING' | 'GENERATED' | 'FAILED' | string;

export interface ChatSessionSummary {
  sessionId: string;
  userId?: string | null;
  title?: string | null;
  titleStatus: ChatSessionTitleStatus;
  titleGeneratedAt?: string | null;
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
  userInput: string;
}

export interface SubmitChatTurnResponse {
  sessionId: string;
  userId?: string | null;
  turnId: string;
  title?: string | null;
  titleStatus: ChatSessionTitleStatus;
  titleGeneratedAt?: string | null;
  status: string;
  sessionCreatedAt: string;
  sessionUpdatedAt: string;
  endedAt?: string | null;
  turnStatus: ChatTurnStatus;
  createdAt: string;
}

export interface ChatTurnStatusResponse {
  turnId: string;
  sessionId: string;
  status: ChatTurnStatus;
  userInput: string;
  assistantOutput?: string | null;
  errorMessage?: string | null;
  createdAt: string;
  completedAt?: string | null;
}

export interface ChatSessionTitleResponse {
  sessionId: string;
  title?: string | null;
  titleStatus: ChatSessionTitleStatus;
  titleGeneratedAt?: string | null;
}
