import { useState } from 'react';
import { Button, Input } from '@arco-design/web-react';
import { IconSend } from '@arco-design/web-react/icon';

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
        <Input.TextArea
          value={value}
          autoSize={{ minRows: 2, maxRows: 5 }}
          placeholder="输入医学问题、病例上下文或需要核对的用药信息"
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
          <span aria-hidden="true" />
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
