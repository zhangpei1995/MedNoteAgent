import dayjs from 'dayjs';
import type { ChatSessionSummary, ChatTurnRecord } from '../types/chat';

export interface ConsultationType {
  label: string;
  tone: 'blue' | 'green' | 'orange' | 'purple' | 'gray';
}

export interface SessionHistoryGroup {
  label: string;
  sessions: ChatSessionSummary[];
}

interface ConsultationTypeRule {
  type: ConsultationType;
  pattern: RegExp;
}

const CONSULTATION_TYPE_RULES: ConsultationTypeRule[] = [
  {
    type: { label: '报告解读', tone: 'purple' },
    pattern: /报告|检查|检验|血常规|尿常规|肝功能|肾功能|指标|影像|CT|MRI|B超|彩超/i,
  },
  {
    type: { label: '用药咨询', tone: 'green' },
    pattern: /用药|药|剂量|服用|降压|降糖|抗生素|不良反应|禁忌|过敏|说明书/i,
  },
  {
    type: { label: '就诊建议', tone: 'orange' },
    pattern: /就诊|挂号|急诊|门诊|科室|医院|医生|是否需要|胸闷|胸痛|呼吸困难/i,
  },
  {
    type: { label: '病历整理', tone: 'gray' },
    pattern: /病历|病史|诊断|出院|入院|主诉|现病史|既往史|整理/i,
  },
  {
    type: { label: '症状咨询', tone: 'blue' },
    pattern: /发热|头痛|咳嗽|腹痛|腹泻|恶心|呕吐|头晕|皮疹|疼|痛|症状|不舒服/i,
  },
];

const FALLBACK_CONSULTATION_TYPE: ConsultationType = { label: '医学咨询', tone: 'blue' };
const WEEKDAY_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

export function inferConsultationType(text: string): ConsultationType {
  const matchedRule = CONSULTATION_TYPE_RULES.find((rule) => rule.pattern.test(text));
  return matchedRule?.type ?? FALLBACK_CONSULTATION_TYPE;
}

export function buildSessionClassificationText(session: ChatSessionSummary): string {
  return session.title || '';
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
    const matches = output.match(/https?:\/\/|\[\d+]|来源|参考|指南|证据/g);
    return count + (matches?.length || 0);
  }, 0);
}

export function hasClinicalRiskSignal(turns: ChatTurnRecord[]): boolean {
  const content = turns.map((turn) => `${turn.userInput}\n${turn.assistantOutput || ''}`).join('\n');
  return /过敏|禁忌|妊娠|孕|儿童|老人|肝|肾|剂量|用药|药物|急诊|胸痛|呼吸困难/.test(content);
}

export function inferSafetyFocusItems(turns: ChatTurnRecord[]): string[] {
  const content = turns.map((turn) => `${turn.userInput}\n${turn.assistantOutput || ''}`).join('\n');
  const items = [
    [/用药|药物|剂量|服用|抗生素|禁忌|说明书/, '用药与剂量'],
    [/过敏|皮疹|荨麻疹|喉头水肿/, '过敏风险'],
    [/妊娠|孕妇|怀孕|哺乳|母乳|喂奶/, '孕哺特殊状态'],
    [/儿童|小儿|婴儿|宝宝|老人|老年/, '特殊年龄人群'],
    [/肝功能|肝病|肝损伤|肾功能|肾病|肾损伤/, '肝肾功能风险'],
    [/急诊|胸痛|呼吸困难|昏迷|抽搐|大出血/, '急症信号'],
  ]
    .filter(([pattern]) => (pattern as RegExp).test(content))
    .map(([, label]) => label as string);

  return Array.from(new Set(items));
}

export function buildTargetUnderstandingText(turns: ChatTurnRecord[], targetFeatures: string[]): string {
  if (turns.length === 0) {
    return '等待首次咨询。发送问题后，这里会展示系统对咨询对象、特殊状态和当前问题的理解。';
  }

  if (targetFeatures.length === 0) {
    return '当前尚未识别到明确的年龄、性别、特殊状态或过敏线索，回答前建议补充咨询对象和关键背景。';
  }

  return `系统可能正在围绕${targetFeatures.join('、')}相关问题组织回答，请核对这些线索是否属于真正的咨询对象。`;
}

export function inferTargetUserFeatures(turns: ChatTurnRecord[]): string[] {
  const content = turns.map((turn) => `${turn.userInput}\n${turn.assistantOutput || ''}`).join('\n');
  const features = [
    [/哺乳|母乳|喂奶/, '哺乳期'],
    [/妊娠|孕妇|怀孕/, '妊娠相关'],
    [/儿童|小儿|婴儿|宝宝/, '儿童'],
    [/老人|老年/, '老年'],
    [/肝功能|肝病|肝损伤/, '肝功能风险'],
    [/肾功能|肾病|肾损伤/, '肾功能风险'],
    [/过敏/, '过敏史待核对'],
  ]
    .filter(([pattern]) => (pattern as RegExp).test(content))
    .map(([, label]) => label as string);

  return Array.from(new Set(features));
}
