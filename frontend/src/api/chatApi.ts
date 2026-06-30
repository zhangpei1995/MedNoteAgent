import axios from 'axios';
import type {
  ChatSessionSummary,
  ChatSessionMetadataResponse,
  ChatTurnRecord,
  ChatTurnStatusResponse,
  SubmitChatTurnRequest,
  SubmitChatTurnResponse,
} from '../types/chat';

const http = axios.create({
  baseURL: '/api',
  timeout: 20000,
});

export const chatApi = {
  async listSessions(keyword?: string): Promise<ChatSessionSummary[]> {
    const response = await http.get<ChatSessionSummary[]>('/chat/sessions', {
      params: keyword ? { keyword } : undefined,
    });
    return response.data;
  },

  async listSessionTurns(sessionId: string): Promise<ChatTurnRecord[]> {
    const response = await http.get<ChatTurnRecord[]>(`/chat/sessions/${sessionId}/turns`);
    return response.data;
  },

  async getSessionMetadata(sessionId: string): Promise<ChatSessionMetadataResponse> {
    const response = await http.get<ChatSessionMetadataResponse>(`/chat/sessions/${sessionId}/metadata`);
    return response.data;
  },

  async submitTurn(request: SubmitChatTurnRequest): Promise<SubmitChatTurnResponse> {
    const response = await http.post<SubmitChatTurnResponse>('/chat/sessions', request);
    return response.data;
  },

  async getTurnStatus(turnId: string): Promise<ChatTurnStatusResponse> {
    const response = await http.get<ChatTurnStatusResponse>(`/chat/turns/${turnId}`);
    return response.data;
  },
};
