import { Button, Space, Tag, Typography } from '@arco-design/web-react';
import { IconMore, IconPlus, IconSafe } from '@arco-design/web-react/icon';
import { ChatInput } from '../components/ChatInput';
import { ChatMessageList } from '../components/ChatMessageList';
import { MedicalContextPanel } from '../components/MedicalContextPanel';
import { SessionSidebar } from '../components/SessionSidebar';
import { useChatWorkspace } from '../hooks/useChatWorkspace';

export function AgentChatPage() {
  const workspace = useChatWorkspace();

  return (
    <div className="app-shell">
      <SessionSidebar
        sessions={workspace.sessions}
        selectedSessionId={workspace.selectedSessionId}
        loading={workspace.sessionLoading}
        searchKeyword={workspace.sessionKeyword}
        onSelectSession={workspace.selectSession}
        onStartNewSession={workspace.startNewSession}
        onRefresh={() => void workspace.refreshSessions()}
        onSearchKeywordChange={workspace.searchSessions}
      />

      <main className="workspace-main">
        <div className="chat-panel">
          <header className="chat-header">
            <div className="chat-title-group">
              <Typography.Title heading={5}>
                {workspace.selectedSession?.title || (workspace.selectedSessionId ? '历史会话' : '新的医学咨询')}
              </Typography.Title>
              <Space className="chat-session-meta" size={8} wrap>
                <Tag size="small" icon={<IconSafe />}>
                  可追溯医学问答
                </Tag>
                <Typography.Text type="secondary">
                  {workspace.selectedSession ? '基于当前咨询内容生成，不能替代医生诊断' : '发送第一条消息后开始咨询'}
                </Typography.Text>
              </Space>
            </div>
            <div className="chat-header-actions">
              <Button className="header-icon-action" icon={<IconMore />} />
              <Button className="header-new-session" icon={<IconPlus />} onClick={workspace.startNewSession}>
                新咨询
              </Button>
            </div>
          </header>

          <section className="chat-content">
            <ChatMessageList
              turns={workspace.turns}
              loading={workspace.turnsLoading}
              pollingTurnId={workspace.pollingTurnId}
            />
          </section>

          <footer className="chat-footer">
            <ChatInput disabled={workspace.sending} onSend={workspace.sendMessage} />
          </footer>
        </div>

        <MedicalContextPanel
          turns={workspace.turns}
          pollingTurnId={workspace.pollingTurnId}
        />
      </main>
    </div>
  );
}
