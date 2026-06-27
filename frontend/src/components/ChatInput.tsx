import { useState } from 'react';
import { Button, Input, Tag, Typography } from '@arco-design/web-react';
import { IconBook, IconCommand, IconSafe, IconSend } from '@arco-design/web-react/icon';

interface ChatInputProps {
  disabled?: boolean;
  onSend: (content: string) => Promise<void>;
}

export function ChatInput({ disabled, onSend }: ChatInputProps) {
  const [value, setValue] = useState('');

  const submit = async () => {
    const content = value.trim();
    if (!content || disabled) {
      return;
    }

    setValue('');
    await onSend(content);
  };

  return (
    <div className="chat-input-shell">
      <div className="chat-input-card">
        <div className="chat-input-topline">
          <Tag className="input-context-tag" size="small" icon={<IconSafe />}>
            药品说明书检索
          </Tag>
          <Typography.Text type="secondary">Enter 发送，Shift + Enter 换行</Typography.Text>
        </div>
        <Input.TextArea
          value={value}
          autoSize={{ minRows: 2, maxRows: 5 }}
          placeholder="输入药品名称或要查询的说明书条目，例如用法用量、禁忌、不良反应"
          disabled={disabled}
          onChange={setValue}
          onPressEnter={(event) => {
            if (!event.shiftKey) {
              event.preventDefault();
              void submit();
            }
          }}
        />
        <div className="chat-input-actions">
          <div className="input-assist-chips" aria-hidden="true">
            <span>
              <IconBook />
              说明书
            </span>
            <span>
              <IconCommand />
              未收录则说明
            </span>
          </div>
          <Button
            className="send-button"
            type="primary"
            icon={<IconSend />}
            loading={disabled}
            disabled={!value.trim()}
            onClick={() => void submit()}
          >
            发送
          </Button>
        </div>
      </div>
    </div>
  );
}
