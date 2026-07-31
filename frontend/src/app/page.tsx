import LibraryHome from '@/components/library-home';

export default function Home() {
  // 🚀 목업 데이터 개발 중
  // 실제로는 라우팅을 통해 libraryId를 받아야 함
  // 현재는 고정된 목업 ID 사용
  const libraryId = 'lib-001';

  return <LibraryHome libraryId={libraryId} />;
}
