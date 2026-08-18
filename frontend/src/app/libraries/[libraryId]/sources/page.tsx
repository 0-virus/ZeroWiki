'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { HeaderBar } from '@/components/HeaderBar';
import {
  ApiError,
  api,
  getApiErrorMessage,
  SourceSummary,
} from '@/lib/api';
import styles from '../../library.module.css';

const MAX_ZIP_BYTES = 1024 * 1024 * 1024;

type UploadStep = 'hashing' | 'session' | 'transferring' | 'completing' | '';

async function sha256File(file: File) {
  const digest = await crypto.subtle.digest('SHA-256', await file.arrayBuffer());
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('');
}

function uploadSessionValue(value: unknown) {
  if (typeof value === 'string' && value.length > 0) return value;
  return null;
}

export default function SourcesPage() {
  const { libraryId } = useParams<{ libraryId: string }>();
  const router = useRouter();
  const [sources, setSources] = useState<SourceSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [sourceError, setSourceError] = useState('');
  const [uploadError, setUploadError] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState('');
  const [uploadStep, setUploadStep] = useState<UploadStep>('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!libraryId) return;

    let active = true;
    setIsLoading(true);
    setSourceError('');

    api
      .listSources(libraryId)
      .then((response) => {
        if (active) setSources(response.data ?? []);
      })
      .catch((requestError) => {
        if (active) setSourceError(getApiErrorMessage(requestError));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [libraryId, reloadKey]);

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setUploadError('');

    if (!file) {
      setUploadError('Obsidian ZIP 파일을 선택해 주세요.');
      return;
    }

    if (!title.trim()) {
      setUploadError('원본 제목을 입력해 주세요.');
      return;
    }

    if (file.size > MAX_ZIP_BYTES) {
      setUploadError('ZIP 파일은 1GB 이하만 업로드할 수 있습니다.');
      return;
    }

    setUploadStep('hashing');
    let uploadId = '';

    try {
      const sha256 = await sha256File(file);
      setUploadStep('session');
      const sessionResponse = await api.createUploadSession({
        fileName: file.name,
        mediaType: file.type || 'application/zip',
        byteSize: file.size,
        sha256,
      });

      uploadId = uploadSessionValue(sessionResponse.data?.uploadId) ?? '';
      const uploadUrl = uploadSessionValue(sessionResponse.data?.uploadUrl);
      if (
        !uploadId ||
        !uploadUrl ||
        !sessionResponse.data?.method ||
        !sessionResponse.data?.requiredHeaders ||
        !sessionResponse.data?.expiresAt
      ) {
        throw new ApiError(502, '업로드 세션 응답에 필요한 정보가 없습니다.', {
          code: 'UPLOAD_CONTRACT_MISSING',
        });
      }

      setUploadStep('transferring');
      await api.uploadPresignedFile(sessionResponse.data, file);
      setUploadStep('completing');
      await api.completeUpload(uploadId, {
        libraryId,
        sourceType: 'OBSIDIAN',
        title: title.trim(),
        startIngest: true,
        processingMode: 'STANDARD',
      });
      router.push(`/libraries/${encodeURIComponent(libraryId)}/ingest`);
    } catch (uploadError) {
      if (uploadId) {
        api.deleteUpload(uploadId).catch(() => undefined);
      }
      setUploadError(getApiErrorMessage(uploadError));
    } finally {
      setUploadStep('');
    }
  }

  const uploadMessage = {
    hashing: '파일 해시를 계산하는 중입니다…',
    session: '업로드 세션을 준비하는 중입니다…',
    transferring: '파일을 저장소로 전송하는 중입니다…',
    completing: '원본 버전을 확정하고 Ingest를 시작하는 중입니다…',
    '': '',
  }[uploadStep];

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
            href={`/libraries/${encodeURIComponent(libraryId)}/ingest`}
          >
            Ingest 작업 보기
          </Link>
        </div>

        <header className={styles.header}>
          <div>
            <h1 className={styles.title}>원본 보관소</h1>
            <p className={styles.muted}>원본은 덮어쓰지 않고 버전으로 보관합니다.</p>
          </div>
        </header>

        <div className={styles.layout}>
          <section className={styles.section} aria-labelledby="source-list-title">
            <h2 id="source-list-title">원본 목록</h2>
            {isLoading && (
              <p className={styles.status} role="status" aria-live="polite">
                원본 목록을 불러오는 중입니다…
              </p>
            )}
            {!isLoading && sourceError && (
              <div className={styles.error} role="alert">
                <p>{sourceError}</p>
                <button
                  className={styles.back}
                  type="button"
                  onClick={() => setReloadKey((key) => key + 1)}
                >
                  다시 시도
                </button>
              </div>
            )}
            {!isLoading && !sourceError && sources.length === 0 && (
              <p className={styles.empty} role="status">
                아직 보관된 원본이 없습니다.
              </p>
            )}
            {!isLoading && !sourceError && sources.length > 0 && (
              <ul className={styles.list}>
                {sources.map((source) => (
                  <li className={styles.item} key={source.id}>
                    <strong className={styles.itemTitle}>{source.title ?? '제목 없는 원본'}</strong>
                    {source.sourceType && (
                      <span className={styles.itemMeta}>유형: {source.sourceType}</span>
                    )}
                    {source.detectedLanguage && (
                      <span className={styles.itemMeta}>
                        감지 언어: {source.detectedLanguage}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className={styles.section} aria-labelledby="obsidian-upload-title">
            <h2 id="obsidian-upload-title">Obsidian Import</h2>
            <p className={styles.fileHint}>
              ZIP 파일의 MIME·크기·압축 해제 검사는 서버가 수행합니다. 최대 1GB까지 선택할 수 있습니다.
            </p>
            <form className={styles.form} onSubmit={handleUpload} aria-busy={Boolean(uploadStep)}>
              <div className={styles.field}>
                <label htmlFor="obsidian-title">원본 제목</label>
                <input
                  id="obsidian-title"
                  value={title}
                  onChange={(event) => setTitle(event.target.value)}
                  required
                  disabled={Boolean(uploadStep)}
                />
              </div>
              <div className={styles.field}>
                <label htmlFor="obsidian-file">Obsidian ZIP 파일</label>
                <input
                  id="obsidian-file"
                  type="file"
                  accept=".zip,application/zip"
                  onChange={(event) => setFile(event.currentTarget.files?.[0] ?? null)}
                  required
                  disabled={Boolean(uploadStep)}
                />
              </div>
              {uploadMessage && (
                <p className={styles.status} role="status" aria-live="polite">
                  {uploadMessage}
                </p>
              )}
              {uploadError && (
                <p className={styles.error} role="alert">
                  {uploadError}
                </p>
              )}
              <button className={styles.submit} type="submit" disabled={Boolean(uploadStep)}>
                {uploadStep ? '업로드 처리 중…' : 'Obsidian Import 시작'}
              </button>
            </form>
          </section>
        </div>
      </main>
    </>
  );
}
