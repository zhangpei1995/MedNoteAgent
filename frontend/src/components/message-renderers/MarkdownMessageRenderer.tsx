import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import type { AssistantMessageRenderer } from './types';

function renderMarkdown(content: string) {
  return (
    <div className="markdown-message">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
    </div>
  );
}

/**
 * Default assistant renderer for structured text returned by the medical agent.
 *
 * GFM is enabled so tables, task lists, and fenced code remain readable while
 * later medical-specific renderers can still take priority when needed.
 */
export const markdownMessageRenderer: AssistantMessageRenderer = {
  id: 'markdown',
  priority: 0,
  supports: (content) => content.trim().length > 0,
  render: renderMarkdown,
};
