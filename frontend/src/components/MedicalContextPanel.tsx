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
import {
  buildTargetUnderstandingText,
  countEvidenceSignals,
  hasClinicalRiskSignal,
  inferSafetyFocusItems,
  inferTargetUserFeatures,
} from '../domain/sessionDisplay';

interface MedicalContextPanelProps {
  turns: ChatTurnRecord[];
  pollingTurnId?: string;
}

export function MedicalContextPanel({ turns, pollingTurnId }: MedicalContextPanelProps) {
  const isProcessing = Boolean(pollingTurnId);
  const evidenceCount = countEvidenceSignals(turns);
  const hasRiskSignal = hasClinicalRiskSignal(turns);
  const targetFeatures = inferTargetUserFeatures(turns);
  const safetyFocusItems = inferSafetyFocusItems(turns);
  const understandingText = buildTargetUnderstandingText(turns, targetFeatures);

  return (
    <aside className="medical-context-panel" aria-label="医学上下文">
      <section className="context-section">
        <div className="context-section-title">
          <IconUser />
          <Typography.Text>咨询上下文</Typography.Text>
        </div>
        <div className="context-understanding-card">
          <div>
            <span>系统当前理解</span>
            <Tag size="small" color={isProcessing ? 'blue' : 'gray'}>
              {isProcessing ? '生成中' : '待核对'}
            </Tag>
          </div>
          <strong>{understandingText}</strong>
          <p>如果识别不准确，请在下一条消息直接补充更正，例如说明“患者是谁、年龄、性别、过敏史或特殊状态”。</p>
        </div>
        <div className="target-feature-card">
          <span>已识别关注点</span>
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
          <Typography.Text>安全与依据</Typography.Text>
        </div>
        <div className="safety-focus-card">
          <span>安全核对</span>
          <div>
            {safetyFocusItems.length > 0 ? (
              safetyFocusItems.map((item) => (
                <Tag key={item} size="small" color="red">
                  {item}
                </Tag>
              ))
            ) : (
              <Tag size="small" color="green">
                暂无明显高风险线索
              </Tag>
            )}
          </div>
        </div>
        <div className={hasRiskSignal ? 'context-alert warning' : 'context-alert'}>
          {hasRiskSignal ? <IconExclamationCircle /> : <IconCheckCircle />}
          <span>
            {hasRiskSignal
              ? '已检测到需要谨慎处理的医学线索，回答应保留禁忌、剂量风险、就医建议和证据来源。'
              : '当前未检测到明显高风险线索，仍建议补充年龄、性别、病程和用药背景。'}
          </span>
        </div>
        <div className="context-source-empty">
          <IconBook />
          <span>
            {evidenceCount > 0
              ? `本会话已检测到 ${evidenceCount} 条来源或证据线索，可回到回答正文核对。`
              : '暂无明确来源线索；涉及诊疗或用药时，建议补充指南、说明书或检查资料后再采信。'}
          </span>
        </div>
      </section>

      <section className="context-section context-note">
        <IconInfoCircle />
        <span>本回答用于辅助理解和准备就医沟通，不能替代医生诊断。若出现急症、严重过敏、呼吸困难或胸痛等情况，请及时就医。</span>
      </section>
    </aside>
  );
}
