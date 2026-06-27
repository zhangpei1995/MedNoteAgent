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
                {workspace.selectedSession?.title || (workspace.selectedSessionId ? '历史检索' : '新的药品说明书检索')}
              </Typography.Title>
              <Space className="chat-session-meta" size={8} wrap>
                <Tag size="small" icon={<IconSafe />}>
                  可追溯说明书检索
                </Tag>
                <Typography.Text type="secondary">
                  {workspace.selectedSession ? '仅基于已录入药品说明书回答' : '发送药品名称后开始检索'}
                </Typography.Text>
              </Space>
            </div>
            <div className="chat-header-actions">
              <Button className="header-icon-action" icon={<IconMore />} />
              <Button className="header-new-session" icon={<IconPlus />} onClick={workspace.startNewSession}>
                新检索
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
