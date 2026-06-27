import { markdownMessageRenderer } from './MarkdownMessageRenderer';
import type { AssistantMessageRenderer } from './types';

const assistantMessageRenderers: AssistantMessageRenderer[] = [markdownMessageRenderer].sort(
  (left, right) => right.priority - left.priority,
);

/**
 * Selects the first registered assistant renderer that supports the content.
 * Add richer medical result renderers here without changing message-list layout.
 */
export function renderAssistantMessage(content: string) {
  const renderer = assistantMessageRenderers.find((candidate) => candidate.supports(content));
  return renderer?.render(content) ?? null;
}
