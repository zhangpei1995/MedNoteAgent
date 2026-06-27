import { Button, Typography } from '@arco-design/web-react';
import { IconPlus, IconSafe } from '@arco-design/web-react/icon';
import { ChatInput } from '../components/ChatInput';
import { ChatMessageList } from '../components/ChatMessageList';
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

      <main className="chat-panel">
        <header className="chat-header">
          <div className="chat-title-group">
            <div className="chat-kicker">
              <IconSafe />
              <Typography.Text>医学问答工作台</Typography.Text>
            </div>
            <Typography.Title heading={4}>
              {workspace.selectedSession?.title || (workspace.selectedSessionId ? '历史会话' : '新的医学咨询')}
            </Typography.Title>
            <Typography.Text type="secondary">
              {workspace.selectedSession ? `会话 ${workspace.selectedSession.sessionId}` : '发送第一条消息后创建会话'}
            </Typography.Text>
          </div>
          <Button className="header-new-session" icon={<IconPlus />} onClick={workspace.startNewSession}>
            新会话
          </Button>
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
      </main>
    </div>
  );
}
