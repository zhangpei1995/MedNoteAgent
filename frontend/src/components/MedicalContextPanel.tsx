import { Tag, Typography } from '@arco-design/web-react';
import {
  IconBook,
  IconCheckCircle,
  IconExclamationCircle,
  IconInfoCircle,
  IconSafe,
  IconUser,
} from '@arco-design/web-react/icon';
import type { ChatSessionSummary, ChatTurnRecord } from '../types/chat';
import {
  countEvidenceSignals,
} from '../domain/sessionDisplay';

interface MedicalContextPanelProps {
  session?: ChatSessionSummary;
  turns: ChatTurnRecord[];
  pollingTurnId?: string;
}

export function MedicalContextPanel({ session, turns, pollingTurnId }: MedicalContextPanelProps) {
  const isProcessing = Boolean(pollingTurnId);
  const evidenceCount = countEvidenceSignals(turns);
  const isMetadataGenerating = session?.metadataStatus === 'GENERATING';
  const hasBoundarySignal = session?.scopeStatus === 'OUT_OF_SCOPE'
    || session?.knowledgeStatus === 'UNKNOWN_DRUG'
    || session?.knowledgeStatus === 'NOT_INCLUDED';
  const targetFeatures = [session?.recognizedDrugName].filter(Boolean) as string[];
  const instructionFocusItems = [session?.instructionItem].filter(Boolean) as string[];
  const understandingText = hasBoundarySignal && targetFeatures.length === 0
    ? '未识别到已收录药品，请核对药品全名。'
    : session?.understandingText
      || '等待首次检索。发送药品名称或说明书条目后展示检索理解。';
  const boundaryText = hasBoundarySignal
    ? '未知或未收录，仅说明边界'
    : '按已录入说明书作答';
  const sourceText = evidenceCount > 0
    ? `${evidenceCount} 条说明书依据`
    : '暂无明确依据线索';

  return (
    <aside className="medical-context-panel" aria-label="药品说明书检索上下文">
      <section className="context-section">
        <div className="context-section-title">
          <IconUser />
          <Typography.Text>检索上下文</Typography.Text>
        </div>
        <div className="context-understanding-card">
          <div>
            <span>系统当前理解</span>
            <Tag size="small" color={isMetadataGenerating || isProcessing ? 'blue' : 'gray'}>
              {isMetadataGenerating || isProcessing ? '生成中' : '待核对'}
            </Tag>
          </div>
          <strong>{understandingText}</strong>
          <p>识别不准时，直接补充药品全名或说明书条目。</p>
        </div>
        <div className="target-feature-card">
          <span>已识别检索对象</span>
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
      </section>

      <section className="context-section">
        <div className="context-section-title">
          <IconSafe />
          <Typography.Text>收录与依据</Typography.Text>
        </div>
        <div className="safety-focus-card">
          <span>说明书条目</span>
          <div>
            {instructionFocusItems.length > 0 ? (
              instructionFocusItems.map((item) => (
                <Tag key={item} size="small" color="blue">
                  {item}
                </Tag>
              ))
            ) : (
              <Tag size="small" color="green">
                等待条目检索
              </Tag>
            )}
          </div>
        </div>
        <div className={hasBoundarySignal ? 'context-alert warning' : 'context-alert'}>
          {hasBoundarySignal ? <IconExclamationCircle /> : <IconCheckCircle />}
          <span>{boundaryText}</span>
        </div>
        <div className="context-status-list">
          <div>
            <IconBook />
            <span>{sourceText}</span>
          </div>
          <div>
            <IconInfoCircle />
            <span>只做说明书事实检索，不替代诊疗建议</span>
          </div>
        </div>
      </section>
    </aside>
  );
}
