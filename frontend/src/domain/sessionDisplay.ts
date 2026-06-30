import dayjs from 'dayjs';
import type { ChatSessionSummary, ChatTurnRecord } from '../types/chat';

export interface ConsultationType {
  label: string;
  tone: 'blue' | 'green' | 'orange' | 'gray';
}

export interface SessionHistoryGroup {
  label: string;
  sessions: ChatSessionSummary[];
}

interface ConsultationTypeRule {
  type: ConsultationType;
  category: string;
}

const CONSULTATION_TYPE_RULES: ConsultationTypeRule[] = [
  { category: 'DRUG_LOOKUP', type: { label: '药品检索', tone: 'blue' } },
  { category: 'USAGE_DOSAGE', type: { label: '用法用量', tone: 'green' } },
  { category: 'CONTRAINDICATION', type: { label: '禁忌慎用', tone: 'orange' } },
  { category: 'ADVERSE_REACTION', type: { label: '不良反应', tone: 'orange' } },
  { category: 'INTERACTION', type: { label: '相互作用', tone: 'green' } },
  { category: 'PRECAUTION', type: { label: '注意事项', tone: 'green' } },
  { category: 'INSTRUCTION_ITEM', type: { label: '说明书条目', tone: 'green' } },
  { category: 'OUT_OF_SCOPE', type: { label: '超出范围', tone: 'gray' } },
];

const FALLBACK_CONSULTATION_TYPE: ConsultationType = { label: '药品检索', tone: 'blue' };
const WEEKDAY_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

export function getSessionConsultationType(session: ChatSessionSummary): ConsultationType {
  const matchedRule = CONSULTATION_TYPE_RULES.find((rule) => rule.category === session.consultationCategory);
  if (!matchedRule && session.consultationCategoryLabel) {
    return { label: session.consultationCategoryLabel, tone: 'blue' };
  }
  return matchedRule?.type ?? FALLBACK_CONSULTATION_TYPE;
}

export function groupSessionsByRecency(sessions: ChatSessionSummary[], now = dayjs()): SessionHistoryGroup[] {
  const groups: SessionHistoryGroup[] = [
    { label: '今天', sessions: [] },
    { label: '近 7 天', sessions: [] },
    { label: '更早', sessions: [] },
  ];

  sessions.forEach((session) => {
    const updatedAt = dayjs(session.updatedAt || session.createdAt);
    if (updatedAt.isSame(now, 'day')) {
      groups[0].sessions.push(session);
      return;
    }

    if (updatedAt.isAfter(now.subtract(7, 'day'), 'day')) {
      groups[1].sessions.push(session);
      return;
    }

    groups[2].sessions.push(session);
  });

  return groups.filter((group) => group.sessions.length > 0);
}

export function formatSessionHistoryTime(session: ChatSessionSummary, now = dayjs()): string {
  const updatedAt = dayjs(session.updatedAt || session.createdAt);

  if (updatedAt.isSame(now, 'day')) {
    return updatedAt.format('HH:mm');
  }

  if (updatedAt.isAfter(now.subtract(7, 'day'), 'day')) {
    return `${WEEKDAY_LABELS[updatedAt.day()]} ${updatedAt.format('HH:mm')}`;
  }

  return updatedAt.format('YYYY-MM-DD HH:mm');
}

export function countEvidenceSignals(turns: ChatTurnRecord[]): number {
  return turns.reduce((count, turn) => {
    const output = turn.assistantOutput || '';
    const matches = output.match(/https?:\/\/|\[\d+]|来源|参考|说明书|条目|依据|原文/g);
    return count + (matches?.length || 0);
  }, 0);
}

export function hasInstructionBoundarySignal(turns: ChatTurnRecord[]): boolean {
  const content = turns.map((turn) => `${turn.userInput}\n${turn.assistantOutput || ''}`).join('\n');
  return /未知药品|未识别|未录入|未收录|资料不足|未检索到|没有找到/.test(content);
}

export function inferInstructionFocusItems(turns: ChatTurnRecord[]): string[] {
  const content = turns.map((turn) => `${turn.userInput}\n${turn.assistantOutput || ''}`).join('\n');
  const items = [
    [/药品名称|未知药品|未识别|二冬汤|菖麻熄风|颗粒|片|胶囊|口服液/, '药品名称'],
    [/用法|用量|剂量|服用/, '用法用量'],
    [/禁忌|慎用|注意事项/, '禁忌与注意事项'],
    [/不良反应|副作用/, '不良反应'],
    [/相互作用|合用/, '药物相互作用'],
    [/未录入|未收录|资料不足|未检索到/, '收录状态'],
  ]
    .filter(([pattern]) => (pattern as RegExp).test(content))
    .map(([, label]) => label as string);

  return Array.from(new Set(items));
}

export function buildInstructionUnderstandingText(turns: ChatTurnRecord[], targetFeatures: string[]): string {
  if (turns.length === 0) {
    return '等待首次检索。发送药品名称或说明书条目后，这里会展示系统对检索对象的理解。';
  }

  if (targetFeatures.length === 0) {
    return '当前尚未识别到明确药品名称。请补充要查询的药品全名，系统仅检索已录入说明书。';
  }

  return `系统可能正在围绕${targetFeatures.join('、')}检索说明书内容，请核对药品名称和条目是否准确。`;
}

export function inferDrugInstructionFeatures(turns: ChatTurnRecord[]): string[] {
  const content = turns.map((turn) => `${turn.userInput}\n${turn.assistantOutput || ''}`).join('\n');
  const features = [
    [/二冬汤颗粒|二冬汤/, '二冬汤颗粒'],
    [/菖麻熄风颗粒|菖麻熄风/, '菖麻熄风颗粒'],
    [/未知药品|未识别|药品名称不明确/, '未知药品'],
    [/未录入|未收录|资料不足|未检索到/, '说明未录入'],
    [/用法|用量|剂量|服用/, '用法用量'],
    [/禁忌|慎用|注意事项/, '禁忌与注意事项'],
    [/不良反应|副作用/, '不良反应'],
    [/相互作用|合用/, '药物相互作用'],
  ]
    .filter(([pattern]) => (pattern as RegExp).test(content))
    .map(([, label]) => label as string);

  return Array.from(new Set(features));
}
