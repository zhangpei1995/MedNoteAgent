import axios from 'axios';
import type {
  ChatSessionSummary,
  ChatSessionTitleResponse,
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

  async getSessionTitle(sessionId: string): Promise<ChatSessionTitleResponse> {
    const response = await http.get<ChatSessionTitleResponse>(`/chat/sessions/${sessionId}/title`);
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
