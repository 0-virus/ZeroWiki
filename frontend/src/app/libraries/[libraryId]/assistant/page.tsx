'use client';

import { FormEvent, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import {
  api,
  ApiError,
  AssistantMessage,
  AssistantProcessingMode,
  ConversationFeedbackRating,
  getApiErrorMessage,
} from '@/lib/api';
import styles from '../../library.module.css';

type ViewRole = 'USER' | 'ASSISTANT';

interface ConversationViewMessage {
  id: string;
  role: ViewRole;
  content: string;
  status: string;
  assistant?: AssistantMessage;
}

const PROCESSING_MODES: Array<[AssistantProcessingMode, string]> = [
  ['FAST', '빠른 답변'],
  ['STANDARD', '표준 답변'],
  ['PRECISE', '정밀 답변'],
];

function formatJson(value: unknown) {
  if (value === null || value === undefined) return '';

  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return '';
  }
}

function statusLabel(status: string) {
  switch (status) {
    case 'QUEUED':
      return '답변 준비 중…';
    case 'GENERATING':
      return '답변 생성 중…';
    case 'COMPLETED':
      return '답변 완료';
    case 'FAILED':
      return '답변 생성 실패';
    default:
      return status || '상태 확인 중…';
  }
}

function readConversationId(value: unknown) {
  if (!value || typeof value !== 'object') return null;
  const record = value as Record<string, unknown>;
  const id = record.id ?? record.conversationId;
  return typeof id === 'string' && id.length > 0 ? id : null;
}

function referencedIds(value: string) {
  return value
    .split(/[\s,]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export default function AssistantPage() {
  const { libraryId } = useParams<{ libraryId: string }>();
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [question, setQuestion] = useState('');
  const [answerMode] = useState('COMPARE');
  const [processingMode, setProcessingMode] = useState<AssistantProcessingMode>('STANDARD');
  const [targetLibraryIds, setTargetLibraryIds] = useState('');
  const [messages, setMessages] = useState<ConversationViewMessage[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saveMessageId, setSaveMessageId] = useState('');
  const [saveTitle, setSaveTitle] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [feedbackComments, setFeedbackComments] = useState<Record<string, string>>({});
  const [feedbackRatings, setFeedbackRatings] = useState<
    Record<string, ConversationFeedbackRating>
  >({});
  const [feedbackSubmittingMessageId, setFeedbackSubmittingMessageId] = useState('');
  const pollingActive = useRef(true);

  useEffect(() => {
    pollingActive.current = true;
    return () => {
      pollingActive.current = false;
    };
  }, []);

  function updateAssistantMessage(messageId: string, assistant: AssistantMessage) {
    setMessages((current) =>
      current.map((message) =>
        message.id === messageId
          ? {
              ...message,
              status: assistant.status,
              content: assistant.content ?? message.content,
              assistant,
            }
          : message,
      ),
    );
  }

  async function pollAssistant(
    activeConversationId: string,
    assistantMessageId: string,
    localMessageId: string,
  ) {
    for (let attempt = 0; attempt < 60 && pollingActive.current; attempt += 1) {
      const response = await api.getConversationMessage(
        libraryId,
        activeConversationId,
        assistantMessageId,
      );
      const assistant = response.data;
      updateAssistantMessage(localMessageId, assistant);

      if (assistant.status === 'COMPLETED' || assistant.status === 'FAILED') return;
      await new Promise((resolve) => window.setTimeout(resolve, 1000));
    }

    if (pollingActive.current) {
      throw new ApiError(504, '답변 생성 시간이 초과되었습니다.', { code: 'AI_TIMEOUT' });
    }
  }

  async function submitQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!libraryId || isSubmitting) return;

    const content = question.trim();
    if (!content) {
      setError('질문을 입력해 주세요.');
      return;
    }

    setIsSubmitting(true);
    setError('');
    setNotice('');
    setSaveMessageId('');

    try {
      let activeConversationId = conversationId;
      if (!activeConversationId) {
        const conversationResponse = await api.createConversation(libraryId);
        activeConversationId = readConversationId(conversationResponse.data);
        if (!activeConversationId) {
          throw new ApiError(502, '대화 생성 응답에 대화 ID가 없습니다.', {
            code: 'INVALID_CONVERSATION_RESPONSE',
          });
        }
        setConversationId(activeConversationId);
      }

      const response = await api.sendConversationMessage(libraryId, activeConversationId, {
        content,
        answerMode,
        processingMode,
        searchScope: {
          currentLibrary: true,
          referencedLibraryIds: referencedIds(targetLibraryIds),
        },
      });
      const { assistantMessageId, status } = response.data;
      const localMessageId = `assistant-${assistantMessageId}`;

      setMessages((current) => [
        ...current,
        { id: `user-${Date.now()}`, role: 'USER', content, status: 'COMPLETED' },
        {
          id: localMessageId,
          role: 'ASSISTANT',
          content: '',
          status,
        },
      ]);
      setQuestion('');
      await pollAssistant(activeConversationId, assistantMessageId, localMessageId);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function submitSaveProposal(event: FormEvent<HTMLFormElement>, message: ConversationViewMessage) {
    event.preventDefault();
    if (!libraryId || !message.assistant || !message.assistant.id) return;

    const title = saveTitle.trim();
    if (!title) {
      setError('새 페이지 제목을 입력해 주세요.');
      return;
    }

    const citationIds = (message.assistant.citations ?? [])
      .map((citation) => citation.id)
      .filter((id): id is string => Boolean(id));
    if (citationIds.length === 0) {
      setError('서버가 반환한 인용 ID가 없어 저장 제안을 만들 수 없습니다.');
      return;
    }

    setIsSaving(true);
    setError('');
    setNotice('');
    try {
      const response = await api.saveConversationMessageProposal(libraryId, message.assistant.id, {
        target: { type: 'NEW_PAGE', pageType: 'SYNTHESIS', title },
        includeCitationIds: citationIds,
      });
      setNotice(
        `저장 제안이 변경 세트(${response.data.changeSetId})로 생성되었습니다. 자동 적용하지 않았습니다.`,
      );
      setSaveMessageId('');
      setSaveTitle('');
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setIsSaving(false);
    }
  }

  async function submitFeedback(message: ConversationViewMessage, rating: ConversationFeedbackRating) {
    if (!libraryId || !message.assistant?.id || feedbackSubmittingMessageId) return;

    const comment = feedbackComments[message.id]?.trim();
    setFeedbackSubmittingMessageId(message.id);
    setError('');
    setNotice('');
    try {
      const response = await api.submitConversationFeedback(libraryId, message.assistant.id, {
        rating,
        ...(comment ? { comment } : {}),
      });
      if (!response.data.accepted) {
        throw new ApiError(502, '답변 평가가 저장되지 않았습니다.', {
          code: 'FEEDBACK_NOT_ACCEPTED',
        });
      }
      setFeedbackRatings((current) => ({ ...current, [message.id]: rating }));
      setNotice('답변 평가가 저장되었습니다.');
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setFeedbackSubmittingMessageId('');
    }
  }

  return (
    <>
      <HeaderBar />
      <main className={styles.page}>
        <div className={styles.actions}>
          <Link className={styles.back} href={`/libraries/${encodeURIComponent(libraryId)}`}>
            도서관으로
          </Link>
          <Link
            className={styles.back}
            href={`/libraries/${encodeURIComponent(libraryId)}/search`}
          >
            검색
          </Link>
        </div>

        <header className={styles.header}>
          <div>
            <h1 className={styles.title}>AI 사서</h1>
            <p className={styles.muted}>
              현재 도서관을 기준으로 답변합니다. 참조 도서관 ID를 입력해야 확장 범위를 요청합니다.
            </p>
          </div>
        </header>

        {notice && (
          <p className={styles.notice} role="status" aria-live="polite">
            {notice}
          </p>
        )}
        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}

        <section className={styles.section} aria-labelledby="assistant-question-title">
          <h2 id="assistant-question-title">질문하기</h2>
          <form className={styles.form} onSubmit={submitQuestion}>
            <div className={styles.field}>
              <label htmlFor="assistant-question">질문</label>
              <textarea
                id="assistant-question"
                value={question}
                onChange={(event) => setQuestion(event.target.value)}
                placeholder="현재 도서관의 근거를 바탕으로 질문해 주세요."
                required
                disabled={isSubmitting}
              />
            </div>
            <div className={styles.fieldset}>
              <legend>답변 설정</legend>
              <div className={styles.field}>
                <label htmlFor="assistant-answer-mode">답변 방식</label>
                <select id="assistant-answer-mode" value={answerMode} disabled>
                  <option value="COMPARE">비교형 답변</option>
                </select>
                <span className={styles.fileHint}>
                  정본에 정의된 답변 방식만 노출합니다.
                </span>
              </div>
              <div className={styles.field}>
                <label htmlFor="assistant-processing-mode">처리 방식</label>
                <select
                  id="assistant-processing-mode"
                  value={processingMode}
                  onChange={(event) =>
                    setProcessingMode(event.target.value as AssistantProcessingMode)
                  }
                  disabled={isSubmitting}
                >
                  {PROCESSING_MODES.map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <div className={styles.field}>
                <label htmlFor="assistant-referenced-library-ids">
                  참조 도서관 ID (선택)
                </label>
                <textarea
                  id="assistant-referenced-library-ids"
                  value={targetLibraryIds}
                  onChange={(event) => setTargetLibraryIds(event.target.value)}
                  placeholder="도서관 ID를 쉼표 또는 줄바꿈으로 구분"
                  disabled={isSubmitting}
                />
                <span className={styles.fileHint}>
                  입력한 도서관만 `searchScope.referencedLibraryIds`에 포함됩니다.
                </span>
              </div>
            </div>
            <button className={styles.submit} type="submit" disabled={isSubmitting}>
              {isSubmitting ? '답변을 기다리는 중…' : '질문 보내기'}
            </button>
          </form>
        </section>

        <section className={styles.section} aria-labelledby="assistant-conversation-title">
          <h2 id="assistant-conversation-title">대화</h2>
          {messages.length === 0 && (
            <p className={styles.empty} role="status">
              질문을 보내면 대화가 여기에 표시됩니다.
            </p>
          )}
          {messages.length > 0 && (
            <div className={styles.assistantConversation} aria-live="polite">
              {messages.map((message) => {
                const assistant = message.assistant;
                const citations = assistant?.citations ?? [];
                const citationIds = citations
                  .map((citation) => citation.id)
                  .filter((id): id is string => Boolean(id));
                const missingEvidence = assistant?.suggestions?.missingEvidence;

                return (
                  <article
                    className={`${styles.message} ${
                      message.role === 'USER' ? styles.messageUser : styles.messageAssistant
                    }`}
                    key={message.id}
                  >
                    <header className={styles.messageHeader}>
                      <strong>{message.role === 'USER' ? '질문' : 'AI 사서 답변'}</strong>
                      {message.role === 'ASSISTANT' && (
                        <span className={styles.statusBadge}>{statusLabel(message.status)}</span>
                      )}
                    </header>
                    {message.content ? (
                      <p className={styles.messageContent}>{message.content}</p>
                    ) : (
                      <p className={styles.status} role="status">
                        {statusLabel(message.status)}
                      </p>
                    )}

                    {assistant?.insufficientEvidence && (
                      <p className={styles.warning} role="status">
                        근거가 충분하지 않습니다. 답변은 모르는 내용을 숨기지 않으며, 필요한 자료를 먼저 확인해 주세요.
                      </p>
                    )}
                    {assistant?.suggestions?.expandToReferencedLibraries && (
                      <p className={styles.warning} role="status">
                        연결된 참조 도서관 확장이 제안되었습니다. 자동으로 확장하지 않았습니다. 위 입력란에서 범위를 명시한 뒤 다시 질문해 주세요.
                      </p>
                    )}
                    {assistant?.suggestions?.webSearchAvailable && (
                      <p className={styles.warning} role="status">
                        웹 검색을 제안할 수 있지만 자동 실행하지 않습니다.
                      </p>
                    )}
                    {missingEvidence !== undefined && formatJson(missingEvidence) && (
                      <details className={styles.citationDisclosure}>
                        <summary>부족한 근거 제안 보기</summary>
                        <pre className={styles.rawBlock}>{formatJson(missingEvidence)}</pre>
                      </details>
                    )}

                    {message.role === 'ASSISTANT' && message.status === 'COMPLETED' && (
                      <>
                        <details className={styles.citationDisclosure}>
                          <summary>인용과 원문 위치 ({citations.length})</summary>
                          {citations.length === 0 ? (
                            <p className={styles.empty}>표시할 인용이 없습니다.</p>
                          ) : (
                            <ol className={styles.citationList}>
                              {citations.map((citation, index) => {
                                const pageHref = citation.pageId
                                  ? `/libraries/${encodeURIComponent(libraryId)}/pages/${encodeURIComponent(citation.pageId)}`
                                  : '';
                                const locator = formatJson(citation.locator);
                                return (
                                  <li key={citation.id ?? `${citation.order}-${index}`}>
                                    {pageHref ? (
                                      <Link href={pageHref}>
                                        {citation.pageTitle ?? citation.pageId}
                                      </Link>
                                    ) : (
                                      citation.pageTitle ?? citation.pageId ?? '페이지 정보 없음'
                                    )}
                                    {citation.claimId && (
                                      <span className={styles.itemMeta}>
                                        주장 ID: {citation.claimId}
                                      </span>
                                    )}
                                    {citation.sourceVersionId && (
                                      <span className={styles.itemMeta}>
                                        원본 버전 ID: {citation.sourceVersionId}
                                      </span>
                                    )}
                                    {locator && (
                                      <pre className={styles.rawBlock}>{locator}</pre>
                                    )}
                                  </li>
                                );
                              })}
                            </ol>
                          )}
                        </details>

                        <div className={styles.assistantActions}>
                          <button
                            className={styles.back}
                            type="button"
                            onClick={() => {
                              setSaveMessageId(message.id);
                              setSaveTitle('');
                              setError('');
                            }}
                            disabled={citationIds.length === 0 || isSaving}
                            aria-describedby={`save-help-${message.id}`}
                          >
                            저장 제안 만들기
                          </button>
                          <span className={styles.fileHint} id={`save-help-${message.id}`}>
                            {citationIds.length === 0
                              ? '응답에 인용 ID가 있어야 저장 제안을 만들 수 있습니다.'
                              : '자동 적용하지 않고 변경 세트로 제안합니다.'}
                          </span>
                        </div>
                        {saveMessageId === message.id && (
                          <form
                            className={styles.reviewForm}
                            onSubmit={(event) => submitSaveProposal(event, message)}
                          >
                            <div className={styles.field}>
                              <label htmlFor={`save-title-${message.id}`}>새 페이지 제목</label>
                              <input
                                id={`save-title-${message.id}`}
                                value={saveTitle}
                                onChange={(event) => setSaveTitle(event.target.value)}
                                required
                                disabled={isSaving}
                              />
                            </div>
                            <div className={styles.inlineActions}>
                              <button className={styles.submit} type="submit" disabled={isSaving}>
                                {isSaving ? '제안 생성 중…' : '변경 세트 제안'}
                              </button>
                              <button
                                className={styles.back}
                                type="button"
                                onClick={() => setSaveMessageId('')}
                                disabled={isSaving}
                              >
                                닫기
                              </button>
                            </div>
                          </form>
                        )}

                        <div className={styles.feedbackPanel} aria-labelledby={`feedback-${message.id}`}>
                          <h3 id={`feedback-${message.id}`}>답변 평가</h3>
                          <p className={styles.reviewHint}>
                            도움이 되었는지 알려 주세요. 의견은 선택적으로 남길 수 있습니다.
                          </p>
                          <div className={styles.field}>
                            <label htmlFor={`feedback-comment-${message.id}`}>의견 (선택)</label>
                            <textarea
                              id={`feedback-comment-${message.id}`}
                              value={feedbackComments[message.id] ?? ''}
                              onChange={(event) =>
                                setFeedbackComments((current) => ({
                                  ...current,
                                  [message.id]: event.target.value,
                                }))
                              }
                              maxLength={200}
                              disabled={Boolean(feedbackSubmittingMessageId)}
                              placeholder="답변에서 좋았거나 아쉬웠던 점을 남겨 주세요."
                            />
                          </div>
                          <div className={styles.inlineActions}>
                            <button
                              className={styles.back}
                              type="button"
                              onClick={() => submitFeedback(message, 'HELPFUL')}
                              disabled={Boolean(feedbackSubmittingMessageId) || Boolean(feedbackRatings[message.id])}
                            >
                              도움이 됐어요
                            </button>
                            <button
                              className={styles.back}
                              type="button"
                              onClick={() => submitFeedback(message, 'NOT_HELPFUL')}
                              disabled={Boolean(feedbackSubmittingMessageId) || Boolean(feedbackRatings[message.id])}
                            >
                              아쉬워요
                            </button>
                          </div>
                          {feedbackSubmittingMessageId === message.id && (
                            <p className={styles.status} role="status" aria-live="polite">
                              평가를 저장하는 중입니다…
                            </p>
                          )}
                          {feedbackRatings[message.id] && (
                            <p className={styles.notice} role="status" aria-live="polite">
                              {feedbackRatings[message.id] === 'HELPFUL'
                                ? '도움이 됐어요로 평가했습니다.'
                                : '아쉬워요로 평가했습니다.'}
                            </p>
                          )}
                        </div>
                      </>
                    )}
                  </article>
                );
              })}
            </div>
          )}
        </section>
      </main>
    </>
  );
}
