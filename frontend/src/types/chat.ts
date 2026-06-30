export type ChatTurnStatus = 'WAITING_AGENT' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | string;
export type ChatSessionMetadataStatus = 'GENERATING' | 'GENERATED' | 'FAILED' | string;

export interface ChatSessionSummary {
  sessionId: string;
  userId?: string | null;
  title?: string | null;
  metadataStatus: ChatSessionMetadataStatus;
  consultationCategory?: string | null;
  consultationCategoryLabel?: string | null;
  recognizedDrugName?: string | null;
  instructionItem?: string | null;
  knowledgeStatus?: string | null;
  knowledgeStatusLabel?: string | null;
  scopeStatus?: string | null;
  scopeStatusLabel?: string | null;
  understandingText?: string | null;
  metadataGeneratedAt?: string | null;
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
  metadataStatus: ChatSessionMetadataStatus;
  metadataGeneratedAt?: string | null;
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

export interface ChatSessionMetadataResponse {
  sessionId: string;
  sourceTurnId?: string | null;
  metadataStatus: ChatSessionMetadataStatus;
  title?: string | null;
  consultationCategory?: string | null;
  consultationCategoryLabel?: string | null;
  recognizedDrugName?: string | null;
  instructionItem?: string | null;
  knowledgeStatus?: string | null;
  knowledgeStatusLabel?: string | null;
  scopeStatus?: string | null;
  scopeStatusLabel?: string | null;
  understandingText?: string | null;
  errorMessage?: string | null;
  generatedAt?: string | null;
}
