import { Button, Empty, Input, Spin, Tag, Tooltip, Typography } from '@arco-design/web-react';
import { IconHistory, IconPlus, IconRefresh } from '@arco-design/web-react/icon';
import {
  buildSessionClassificationText,
  formatSessionHistoryTime,
  groupSessionsByRecency,
  inferConsultationType,
} from '../domain/sessionDisplay';
import type { ChatSessionSummary } from '../types/chat';

const { Search } = Input;

interface SessionSidebarProps {
  sessions: ChatSessionSummary[];
  selectedSessionId?: string;
  loading: boolean;
  searchKeyword: string;
  onSelectSession: (sessionId: string) => void;
  onStartNewSession: () => void;
  onRefresh: () => void;
  onSearchKeywordChange: (keyword: string) => void;
}

export function SessionSidebar({
  sessions,
  selectedSessionId,
  loading,
  searchKeyword,
  onSelectSession,
  onStartNewSession,
  onRefresh,
  onSearchKeywordChange,
}: SessionSidebarProps) {
  const emptyDescription = searchKeyword.trim() ? '未找到相关咨询' : '暂无咨询记录';
  const historyGroups = groupSessionsByRecency(sessions);

  return (
    <aside className="session-sidebar">
      <div className="sidebar-header">
        <div>
          <Typography.Title heading={5}>MedNote Agent</Typography.Title>
          <Typography.Text type="secondary">医学咨询工作台</Typography.Text>
        </div>
      </div>

      <Button className="sidebar-nav-action" icon={<IconPlus />} onClick={onStartNewSession}>
        开始新咨询
      </Button>

      <Search
        allowClear
        className="session-search"
        placeholder="搜索症状、疾病、用药或历史咨询"
        searchButton={false}
        value={searchKeyword}
        onChange={onSearchKeywordChange}
      />

      <div className="sidebar-section-label">
        <span>
          <IconHistory />
          历史咨询
        </span>
        <Tooltip content="刷新历史咨询">
          <Button className="sidebar-refresh-action" icon={<IconRefresh />} onClick={onRefresh} />
        </Tooltip>
      </div>

      <Spin loading={loading} block>
        {sessions.length === 0 ? (
          <Empty className="sidebar-empty" description={emptyDescription} />
        ) : (
          <div className="session-history-list">
            {historyGroups.map((group) => (
              <section className="session-history-group" key={group.label}>
                <div className="session-history-group-label">{group.label}</div>
                {group.sessions.map((session) => {
                  const consultationType = inferConsultationType(buildSessionClassificationText(session));

                  return (
                    <button
                      key={session.sessionId}
                      className={session.sessionId === selectedSessionId ? 'session-item active' : 'session-item'}
                      type="button"
                      onClick={() => onSelectSession(session.sessionId)}
                    >
                      <span className="session-item-main">
                        <span className="session-copy">
                          <span className="session-meta-row">
                            <Tag className={`consultation-type-tag tone-${consultationType.tone}`} size="small">
                              {consultationType.label}
                            </Tag>
                            <Typography.Text className="session-time" type="secondary">
                              {formatSessionHistoryTime(session)}
                            </Typography.Text>
                          </span>
                          <Tooltip content={session.title || '未命名咨询'}>
                            <Typography.Text className="session-title" ellipsis title={session.title || '未命名咨询'}>
                              {session.title || '未命名咨询'}
                            </Typography.Text>
                          </Tooltip>
                        </span>
                      </span>
                    </button>
                  );
                })}
              </section>
            ))}
          </div>
        )}
      </Spin>
    </aside>
  );
}
