import { Button, Empty, List, Spin, Tag, Tooltip, Typography } from '@arco-design/web-react';
import { IconMessage, IconPlus, IconRefresh } from '@arco-design/web-react/icon';
import dayjs from 'dayjs';
import type { ChatSessionSummary } from '../types/chat';

interface SessionSidebarProps {
  sessions: ChatSessionSummary[];
  selectedSessionId?: string;
  loading: boolean;
  onSelectSession: (sessionId: string) => void;
  onStartNewSession: () => void;
  onRefresh: () => void;
}

export function SessionSidebar({
  sessions,
  selectedSessionId,
  loading,
  onSelectSession,
  onStartNewSession,
  onRefresh,
}: SessionSidebarProps) {
  return (
    <aside className="session-sidebar">
      <div className="sidebar-header">
        <div>
          <Typography.Title heading={5}>MedNote Agent</Typography.Title>
          <Typography.Text type="secondary">医学咨询会话</Typography.Text>
        </div>
        <div className="sidebar-actions">
          <Tooltip content="刷新会话">
            <Button icon={<IconRefresh />} shape="circle" size="small" onClick={onRefresh} />
          </Tooltip>
          <Tooltip content="新建会话">
            <Button icon={<IconPlus />} shape="circle" size="small" type="primary" onClick={onStartNewSession} />
          </Tooltip>
        </div>
      </div>

      <Spin loading={loading} block>
        {sessions.length === 0 ? (
          <Empty className="sidebar-empty" description="暂无会话" />
        ) : (
          <List
            className="session-list"
            dataSource={sessions}
            render={(session) => (
              <List.Item
                key={session.sessionId}
                className={session.sessionId === selectedSessionId ? 'session-item active' : 'session-item'}
                onClick={() => onSelectSession(session.sessionId)}
              >
                <div className="session-item-main">
                  <IconMessage className="session-icon" />
                  <div className="session-copy">
                    <Typography.Text className="session-title" ellipsis>
                      {session.title || '未命名会话'}
                    </Typography.Text>
                    <Typography.Text className="session-time" type="secondary">
                      {dayjs(session.updatedAt || session.createdAt).format('MM-DD HH:mm')}
                    </Typography.Text>
                  </div>
                </div>
                <Tag size="small" color={session.status === 'ACTIVE' ? 'green' : 'gray'}>
                  {session.status}
                </Tag>
              </List.Item>
            )}
          />
        )}
      </Spin>
    </aside>
  );
}
