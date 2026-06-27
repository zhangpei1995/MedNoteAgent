import { Empty, Spin, Tag, Typography } from '@arco-design/web-react';
import { IconLoading } from '@arco-design/web-react/icon';
import dayjs from 'dayjs';
import { renderAssistantMessage } from './message-renderers/rendererRegistry';
import type { ChatTurnRecord } from '../types/chat';

interface ChatMessageListProps {
  turns: ChatTurnRecord[];
  loading: boolean;
  pollingTurnId?: string;
}

function shouldShowAssistantLoading(turn: ChatTurnRecord, pollingTurnId?: string): boolean {
  return turn.turnId === pollingTurnId || (!turn.assistantOutput && turn.status !== 'FAILED');
}

function statusColor(status: string): string {
  if (status === 'SUCCESS') {
    return 'green';
  }
  if (status === 'FAILED') {
    return 'red';
  }
  return 'arcoblue';
}

export function ChatMessageList({ turns, loading, pollingTurnId }: ChatMessageListProps) {
  if (loading) {
    return (
      <div className="message-loading">
        <Spin loading />
      </div>
    );
  }

  if (turns.length === 0) {
    return (
      <div className="message-empty">
        <Empty description="选择历史会话，或开始新的医学咨询" />
      </div>
    );
  }

  return (
    <div className="message-list">
      {turns.map((turn) => (
        <div className="turn-block" key={turn.turnId}>
          <div className="message-row user">
            <div className="message-bubble">
              <div className="message-meta">
                <Typography.Text type="secondary">咨询输入</Typography.Text>
                <Typography.Text type="secondary">{dayjs(turn.createdAt).format('HH:mm:ss')}</Typography.Text>
              </div>
              <Typography.Paragraph className="message-text">{turn.userInput}</Typography.Paragraph>
            </div>
          </div>

          <div className="message-row assistant">
            <div className="message-bubble">
              <div className="message-meta">
                <Typography.Text type="secondary">医学 Agent</Typography.Text>
                <Tag className="message-status" size="small" color={statusColor(turn.status)}>
                  {turn.status}
                </Tag>
              </div>
              {turn.status === 'FAILED' ? (
                <Typography.Paragraph className="message-text error">
                  {turn.errorMessage || '本轮执行失败，请稍后重试。'}
                </Typography.Paragraph>
              ) : shouldShowAssistantLoading(turn, pollingTurnId) ? (
                <div className="assistant-loading">
                  <IconLoading spin />
                  <Typography.Text type="secondary">正在检索上下文并生成可追溯回答</Typography.Text>
                </div>
              ) : (
                renderAssistantMessage(turn.assistantOutput || '暂无输出')
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
