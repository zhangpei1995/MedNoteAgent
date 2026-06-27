import type { ReactNode } from 'react';

/**
 * Describes a pluggable renderer for assistant message content.
 *
 * Renderers are evaluated by priority from high to low. The first renderer whose
 * `supports` method accepts the message owns the final presentation.
 */
export interface AssistantMessageRenderer {
  /**
   * Stable renderer identifier used for diagnostics and future configuration.
   */
  readonly id: string;

  /**
   * Higher priority renderers are selected first when multiple renderers support
   * the same content.
   */
  readonly priority: number;

  /**
   * Returns whether this renderer should present the current assistant output.
   */
  supports: (content: string) => boolean;

  /**
   * Renders the assistant output. Implementations must keep medical evidence,
   * warnings, citations, and audit text visible in the final UI.
   */
  render: (content: string) => ReactNode;
}
