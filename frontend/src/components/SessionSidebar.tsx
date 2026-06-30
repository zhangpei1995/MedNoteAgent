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
const DEFAULT_SESSION_TITLE = '新问题';

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
  const emptyDescription = searchKeyword.trim() ? '未找到相关检索' : '暂无检索记录';
  const historyGroups = groupSessionsByRecency(sessions);

  return (
    <aside className="session-sidebar">
      <div className="sidebar-header">
        <div>
          <Typography.Title heading={5}>MedNote Agent</Typography.Title>
          <Typography.Text type="secondary">药品说明书检索工作台</Typography.Text>
        </div>
      </div>

      <Button className="sidebar-nav-action" icon={<IconPlus />} onClick={onStartNewSession}>
        开始新检索
      </Button>

      <Search
        allowClear
        className="session-search"
        placeholder="搜索药品、说明书条目或历史检索"
        searchButton={false}
        value={searchKeyword}
        onChange={onSearchKeywordChange}
      />

      <div className="sidebar-section-label">
        <span>
          <IconHistory />
          历史检索
        </span>
        <Tooltip content="刷新历史检索">
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
                  const sessionTitle = session.title ?? DEFAULT_SESSION_TITLE;

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
                            <Tag
                              className={`consultation-type-tag tone-${consultationType.tone}`}
                              size="small"
                              bordered={false}
                            >
                              {consultationType.label}
                            </Tag>
                            <Typography.Text className="session-time" type="secondary">
                              {formatSessionHistoryTime(session)}
                            </Typography.Text>
                          </span>
                          <Tooltip content={sessionTitle}>
                            <Typography.Text className="session-title" ellipsis title={sessionTitle}>
                              {sessionTitle}
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
