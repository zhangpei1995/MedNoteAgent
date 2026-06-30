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
  const understandingText = session?.understandingText
    || '等待首次检索。发送药品名称或说明书条目后，这里会展示系统对检索对象的理解。';

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
          <p>如果识别不准确，请在下一条消息直接补充药品全名或要查询的说明书条目。</p>
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
          <span>
            {hasBoundarySignal
              ? '已检测到未知药品或说明未录入线索，回答应明确说明收录边界，不补充医学判断。'
              : '当前仅按已录入药品说明书检索，不进行诊断、治疗或个体化用药判断。'}
          </span>
        </div>
        <div className="context-source-empty">
          <IconBook />
          <span>
            {evidenceCount > 0
              ? `本会话已检测到 ${evidenceCount} 条说明书来源或依据线索，可回到回答正文核对。`
              : '暂无明确说明书依据线索；未命中已录入资料时应回答未知药品或药品说明未录入。'}
          </span>
        </div>
      </section>

      <section className="context-section context-note">
        <IconInfoCircle />
        <span>当前阶段只做药品说明书事实检索，不判断疾病、不给治疗方案，也不替代医生或药师的个体化用药建议。</span>
      </section>
    </aside>
  );
}
