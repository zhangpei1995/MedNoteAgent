import { Tag, Typography } from '@arco-design/web-react';
import {
  IconBook,
  IconCheckCircle,
  IconExclamationCircle,
  IconInfoCircle,
  IconSafe,
  IconUser,
} from '@arco-design/web-react/icon';
import type { ChatTurnRecord } from '../types/chat';
import { countEvidenceSignals, hasClinicalRiskSignal, inferTargetUserFeatures } from '../domain/sessionDisplay';

interface MedicalContextPanelProps {
  turns: ChatTurnRecord[];
  pollingTurnId?: string;
}

export function MedicalContextPanel({ turns, pollingTurnId }: MedicalContextPanelProps) {
  const isProcessing = Boolean(pollingTurnId);
  const evidenceCount = countEvidenceSignals(turns);
  const hasRiskSignal = hasClinicalRiskSignal(turns);
  const targetFeatures = inferTargetUserFeatures(turns);

  return (
    <aside className="medical-context-panel" aria-label="医学上下文">
      <section className="context-section">
        <div className="context-section-title">
          <IconUser />
          <Typography.Text>当前对话目标用户特征</Typography.Text>
        </div>
        <div className="target-feature-card">
          <span>识别特征</span>
          <div>
            {targetFeatures.length > 0 ? (
              targetFeatures.map((feature) => (
                <Tag key={feature} size="small" color="orange">
                  {feature}
                </Tag>
              ))
            ) : (
              <Tag size="small" color="gray">
                暂未识别
              </Tag>
            )}
          </div>
        </div>
        <div className="context-compact-grid">
          <div>
            <span>基础信息</span>
            <strong>年龄 / 性别待补充</strong>
          </div>
          <div>
            <span>当前状态</span>
            <strong>{isProcessing ? '正在生成回复' : turns.length > 0 ? '可继续补充问题' : '等待首次咨询'}</strong>
          </div>
        </div>
      </section>

      <section className="context-section">
        <div className="context-section-title">
          <IconSafe />
          <Typography.Text>医学安全</Typography.Text>
        </div>
        <div className="context-metric-grid">
          <div className="context-metric">
            <span>证据线索</span>
            <strong>{evidenceCount}</strong>
          </div>
          <div className="context-metric">
            <span>风险提示</span>
            <strong>{hasRiskSignal ? 1 : 0}</strong>
          </div>
        </div>
        <div className={hasRiskSignal ? 'context-alert warning' : 'context-alert'}>
          {hasRiskSignal ? <IconExclamationCircle /> : <IconCheckCircle />}
          <span>{hasRiskSignal ? '本会话包含用药或特殊风险关键词，回复需保留禁忌和证据。' : '暂无明显高风险关键词。'}</span>
        </div>
      </section>

      <section className="context-section">
        <div className="context-section-title">
          <IconBook />
          <Typography.Text>回答依据</Typography.Text>
        </div>
        <div className="context-source-empty">
          <IconBook />
          <span>{evidenceCount > 0 ? '回复中已检测到来源或证据线索。' : '暂无可展示来源。'}</span>
        </div>
      </section>

      <section className="context-section context-note">
        <IconInfoCircle />
        <span>医学建议仅作辅助参考，关键诊疗决策需结合临床检查和医生判断。</span>
      </section>
    </aside>
  );
}
