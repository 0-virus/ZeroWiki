"""Phase 1 M7 security and accessibility gate.

This is a cross-cutting gate, not a second copy of the M1/M2/M4/M6 feature
suites.  Static checks inventory the current backend routes and the frontend
surface; live checks add only the descendant-owner boundary probe.  Upload
secret rejection, change-apply audit creation, refresh rotation, reference
direction, and AI idempotency are executed by the existing M1/M2/M4/M6 live
tests and mapped here.

The API contract is authoritative.  A schema-only ``idempotency_records``
table is not treated as an implementation: absence of Java
``Idempotency-Key`` handling is an intentional FAIL until the backend adds it.

Examples (PowerShell)::

    python qa/test_m7_security_blackbox.py --self-check
    python -m unittest discover -s qa -p "test_m7_security_blackbox.py" -v

When the API is unavailable, live checks remain visible as BLOCKED skips;
static security and accessibility checks still execute.
"""

from __future__ import annotations

import re
import sys
import unittest
import urllib.parse
from pathlib import Path

from test_m1_m2_blackbox import (
    API_SPEC,
    BACKEND_SRC,
    LiveBlackBoxBase,
    ApiClient,
    _data,
    _id_from,
    _new_identity,
)


ROOT = Path(__file__).resolve().parents[1]
REQUIREMENTS = ROOT / "docs" / "ZeroWiki-요구사항-정의서.md"
CHECKLIST = ROOT / "docs" / "검증-QA-계약-확정-체크리스트.md"
SCENARIOS = ROOT / "qa" / "test_scenarios.md"
FRONTEND_SRC = ROOT / "frontend" / "src"

BOUNDARY_STATUS = (403, 404, 422)


SECURITY_MAPPING = (
    ("S-1", "NFR-SEC-06", "15절 6번·15절 8번", "Phase 1"),
    ("S-2", "FR-INP-12", "15절 4번", "Phase 1"),
    ("S-3", "FR-INP-13", "15절 5번", "Phase 2"),
    ("S-4", "NFR-SEC-04·NFR-SEC-05", "15절 9번", "Phase 1"),
    ("S-5", "NFR-SEC-01·NFR-SEC-02", "15절 1번", "Phase 1"),
    ("S-6", "NFR-SEC-07", "14.1", "Phase 3"),
    ("S-7", "NFR-SEC-08·NFR-OPS-03·FR-ING-24", "2.5·15절 10번·15절 12번", "Phase 1"),
    ("S-8", "FR-ACC-02", "2.1·3.1", "Phase 1"),
    ("S-9", "NFR-SEC-07", "14.1", "Phase 3"),
    ("S-10", "NFR-SEC-09", "15절 11번", "Phase 1"),
    ("S-11", "FR-SRC-05", "15절 13번", "Phase 1"),
    ("S-12", "NFR-SEC-03·FR-LIB-04·FR-LIB-05·FR-LIB-07·FR-ASK-07", "15절 14번", "Phase 1"),
    ("S-13", "NFR-SEC-01·FR-PUB-01·FR-PUB-05", "15절 15번", "Phase 3"),
    ("S-14", "NFR-SEC-09·FR-ING-24", "2.5·15절 2번·15절 12번", "Phase 2"),
    ("S-15", "FR-INP-03·UD-18", "6.2", "Phase 2"),
    ("S-16", "FR-NTF-04·UD-23", "11", "Phase 2"),
)


REPRESENTATIVE_FRONTEND = (
    Path("app/login/LoginForm.tsx"),
    Path("app/libraries/page.tsx"),
    Path("app/libraries/[libraryId]/assistant/page.tsx"),
    Path("app/libraries/[libraryId]/search/page.tsx"),
    Path("app/libraries/[libraryId]/sources/page.tsx"),
    Path("app/libraries/[libraryId]/change-sets/page.tsx"),
    Path("app/libraries/[libraryId]/pages/[pageId]/page.tsx"),
)


def _read_text(path: Path) -> str:
    if not path.is_file():
        raise AssertionError(f"required QA input is missing: {path}")
    return path.read_text(encoding="utf-8")


class StaticM7SecurityAccessibilityTest(unittest.TestCase):
    """Static contract, route, redaction, audit, and frontend a11y gate."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.api_text = _read_text(API_SPEC)
        cls.requirements_text = _read_text(REQUIREMENTS)
        cls.checklist_text = _read_text(CHECKLIST)
        cls.scenarios_text = _read_text(SCENARIOS)
        cls.backend_files = list(BACKEND_SRC.rglob("*.java"))
        cls.backend_text = "\n".join(_read_text(path) for path in cls.backend_files)
        migration_root = ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration"
        cls.migration_text = "\n".join(_read_text(path) for path in migration_root.glob("*.sql"))
        cls.frontend_files = list(FRONTEND_SRC.rglob("*.tsx")) if FRONTEND_SRC.is_dir() else []
        cls.css_text = "\n".join(
            _read_text(path) for path in FRONTEND_SRC.rglob("*.css")
        ) if FRONTEND_SRC.is_dir() else ""

    def test_security_mapping_preserves_s_ids_and_phase_scope(self) -> None:
        """NFR/API/Phase is explicit without renumbering existing S scenarios."""
        for scenario_id, nfr, api, phase in SECURITY_MAPPING:
            self.assertIn(scenario_id, self.scenarios_text)
            self.assertIn(scenario_id, self.checklist_text)
            self.assertIn(phase, self.scenarios_text, f"{scenario_id} phase is not recorded")
            for fragment in re.split(r"[·]", nfr):
                self.assertIn(fragment, self.requirements_text)
            for fragment in re.split(r"[·]", api):
                self.assertIn(fragment, self.api_text + self.checklist_text + self.scenarios_text)
        for term in (
            "M7 Phase 1 보안 게이트",
            "NFR-SEC-01",
            "NFR-SEC-02",
            "NFR-SEC-03",
            "NFR-SEC-04",
            "NFR-SEC-05",
            "NFR-SEC-06",
            "NFR-SEC-08",
            "NFR-SEC-09",
            "API 15절",
            "Idempotency-Key",
            "IDEMPOTENCY_KEY_REUSED",
            "IDEMPOTENCY_KEY_IN_PROGRESS",
        ):
            self.assertIn(term, self.api_text + self.requirements_text + self.checklist_text + self.scenarios_text)

    def test_current_backend_security_routes_and_owner_queries_are_present(self) -> None:
        required_routes = (
            '@RequestMapping("/libraries/{libraryId}/pages")',
            '@RequestMapping("/libraries/{libraryId}/ingest-jobs")',
            '@RequestMapping("/libraries/{libraryId}")',
            '@RequestMapping("/uploads")',
            '@PostMapping("/{uploadId}/complete")',
            '@RequestMapping("/sources")',
            '@RequestMapping("/libraries/{libraryId}/relations")',
            '@RequestMapping("/libraries/{libraryId}/contradictions")',
            '@PostMapping("/change-sets/{changeSetId}/reviews")',
            '@PostMapping("/change-sets/{changeSetId}/apply")',
            '@GetMapping("/change-sets")',
            '@GetMapping("/search")',
            '@PostMapping("/search-expansions")',
            '@PostMapping("/conversations/{conversationId}/messages")',
        )
        missing_routes = [route for route in required_routes if route not in self.backend_text]
        self.assertEqual([], missing_routes, f"M7 backend route inventory missing: {missing_routes}")

        required_owner_markers = (
            "findByIdAndOwnerId",
            "findByIdAndLibraryId",
            "findByIdAndLibraryIdAndRequestedBy",
            "requireLibrary(ownerId",
            "findBySourceLibraryIdAndTargetLibraryId",
            "library_references",
        )
        missing_owner_markers = [marker for marker in required_owner_markers if marker not in self.backend_text]
        self.assertEqual([], missing_owner_markers, f"owner/reference enforcement markers missing: {missing_owner_markers}")

    def test_source_secret_prompt_and_redaction_contract_is_enforced(self) -> None:
        required_terms = (
            "SECRET_DETECTED",
            "SecretDetector",
            "FR-ING-17",
            "FR-ING-18",
            "원본 문서 본문은 LLM 시스템·개발자 명령과 분리된 데이터 영역으로 전달",
            "원본에서 발견한 명령문을 시스템 명령으로 실행하지 않는다",
            "오류 응답과 로그에 원본 본문, 토큰, 서명 URL을 포함하지 않는다",
            "GlobalExceptionHandler",
        )
        source = self.api_text + self.requirements_text + self.checklist_text
        missing = [term for term in required_terms if term not in source and term not in self.backend_text]
        self.assertEqual([], missing, f"source/prompt/redaction contract terms missing: {missing}")

        upload_service = _read_text(BACKEND_SRC / "com" / "zerowiki" / "source" / "UploadService.java")
        raw_branch = upload_service[upload_service.find("} else {") : upload_service.find("session.markCompleted")]
        raw_detect = raw_branch.find("SecretDetector.containsSecret(content)")
        source_persist = raw_branch.find("sourceService.create")
        self.assertGreaterEqual(raw_detect, 0)
        self.assertGreater(source_persist, raw_detect, "secret detection must precede source persistence")
        self.assertRegex(
            upload_service,
            re.compile(
                r"SecretDetector\.containsSecret\(content\).*?"
                r"session\.markBlockedSecret\(\).*?"
                r"objectStore\.delete\(session\.getStorageKey\(\)\).*?"
                r"SECRET_DETECTED",
                re.DOTALL,
            ),
        )

        dangerous_execution = re.compile(
            r"Runtime\.getRuntime|ProcessBuilder|ScriptEngine|/bin/sh|cmd\.exe",
            re.IGNORECASE,
        )
        execution_files = [
            path for path in self.backend_files
            if "com\\zerowiki\\ingest" in str(path) or "com\\zerowiki\\assistant" in str(path)
        ]
        execution_hits = [str(path) for path in execution_files if dangerous_execution.search(_read_text(path))]
        self.assertEqual([], execution_hits, f"source body command execution surface found: {execution_hits}")

        log_leak = re.compile(
            r"(?i)(?:logger|log|system\.out|print(?:ln)?)\s*(?:\.|\()([^\n;]*"
            r"(?:uploadUrl|signedUrl|presigned|rawToken|Authorization|Bearer)[^\n;]*)"
        )
        log_hits = [line for line in self.backend_text.splitlines() if log_leak.search(line)]
        self.assertEqual([], log_hits, f"token/signed URL log leak candidates: {log_hits}")
        global_errors = _read_text(BACKEND_SRC / "com" / "zerowiki" / "common" / "GlobalExceptionHandler.java")
        self.assertIn("요청을 처리하지 못했습니다.", global_errors)
        self.assertIn("RequestIdFilter.REQUEST_ID", global_errors)

    def test_change_apply_audit_is_append_only_and_actor_scoped(self) -> None:
        audit_entity = _read_text(BACKEND_SRC / "com" / "zerowiki" / "knowledge" / "AuditLogEntity.java")
        change_service = _read_text(BACKEND_SRC / "com" / "zerowiki" / "knowledge" / "ChangeSetService.java")
        audit_migration = _read_text(ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V5__audit_logs.sql")
        for marker in (
            "actorUserId",
            "libraryId",
            "eventType",
            "resourceId",
            "result",
            "createdAt",
            "auditLogRepository.save",
            "AuditLogEntity.success",
            "reject_audit_log_mutation",
            "BEFORE UPDATE OR DELETE",
        ):
            self.assertIn(marker, audit_entity + change_service + audit_migration)
        self.assertIn("CHANGE_APPLIED", audit_entity)
        self.assertIn("SUCCESS", audit_entity)

    def test_idempotency_implementation_is_not_silently_passed(self) -> None:
        """Fail until API §2.5 is wired; migration alone is not enough."""
        for marker in (
            "idempotency_records",
            "UNIQUE (user_id, idempotency_key)",
            "같은 키·같은 본문 해시",
            "같은 키·다른 본문 해시",
        ):
            self.assertIn(marker, self.migration_text + self.api_text)
        implementation_markers = (
            "Idempotency-Key",
            "IDEMPOTENCY_KEY_REUSED",
            "IDEMPOTENCY_KEY_IN_PROGRESS",
        )
        missing = [marker for marker in implementation_markers if marker not in self.backend_text]
        self.assertEqual(
            [],
            missing,
            "FAIL_M7_IDEMPOTENCY_IMPLEMENTATION_MISSING: idempotency_records is schema-only; "
            f"backend Java is missing {missing}",
        )

    def test_frontend_static_accessibility_gate(self) -> None:
        required = [FRONTEND_SRC / relative for relative in REPRESENTATIVE_FRONTEND]
        missing_files = [str(path) for path in required if not path.is_file()]
        self.assertEqual([], missing_files, f"frontend a11y representative files missing: {missing_files}")

        header = _read_text(FRONTEND_SRC / "components" / "HeaderBar.tsx")
        for marker in ('<header', '<nav', 'aria-label='):
            self.assertIn(marker, header, f"global landmark/navigation marker missing: {marker}")
        for path in required:
            text = _read_text(path)
            self.assertIn("<main", text, f"page lacks a main landmark: {path}")
            self.assertIn("role=\"alert\"", text, f"page lacks an error alert state: {path}")
            self.assertTrue(
                'role="status"' in text or "getApiErrorMessage" in text,
                f"page lacks a loading/status or API error state: {path}",
            )
            fixed_ids = set(re.findall(
                r'<(?:input|textarea|select)\b[^>]*?\bid="([A-Za-z0-9_-]+)"',
                text,
                flags=re.DOTALL,
            ))
            labelled_ids = set(re.findall(r'\bhtmlFor="([A-Za-z0-9_-]+)"', text))
            self.assertTrue(
                fixed_ids.issubset(labelled_ids),
                f"unlabelled fixed form controls in {path}: {sorted(fixed_ids - labelled_ids)}",
            )
            if "id={`" in text:
                self.assertIn("htmlFor={`", text, f"dynamic controls lack a matching label pattern: {path}")

        custom_click = re.compile(r"<(?:div|span|li|p|section|article)\b[^>\n]*\bonClick\s*=")
        custom_click_hits = [str(path) for path in self.frontend_files if custom_click.search(_read_text(path))]
        self.assertEqual([], custom_click_hits, f"non-native clickable element lacks keyboard semantics: {custom_click_hits}")
        self.assertRegex(self.css_text, r":focus(?:-visible)?\b")
        self.assertIn(":focus-visible", self.css_text, "focus-visible styling is missing")

    def test_existing_live_suites_are_the_m7_execution_sources(self) -> None:
        expected_coverage = {
            "qa/test_m1_m2_blackbox.py": (
                "test_library_private_owner_isolation_constitution_and_directional_reference",
                "test_source_secret_rejection_and_prompt_text_is_data",
            ),
            "qa/test_m4_change_review_blackbox.py": ("test_change_review_apply_version_and_owner_boundary",),
            "qa/test_m6_search_ai_blackbox.py": (
                "test_search_modes_expansion_and_owner_reference_boundary",
                "test_ai_question_idempotency_key_reuses_the_same_result",
            ),
        }
        for relative, markers in expected_coverage.items():
            text = _read_text(ROOT / relative)
            for marker in markers:
                self.assertIn(marker, text, f"M7 live coverage source missing: {relative}::{marker}")


class M7LiveBlackBoxTest(LiveBlackBoxBase):
    """One additional owner-descendant probe; feature flows remain in M1/M2/M4/M6."""

    def test_owner_boundary_covers_current_library_descendant_routes(self) -> None:
        _, owner_token, _ = self.sign_up_and_login("m7-owner")
        library_id, _ = self.create_library(owner_token, "M7 owner boundary")
        other_api = ApiClient()
        identity = _new_identity("m7-other")
        signup = other_api.request("POST", "/auth/sign-up", body=identity)
        self.assert_status(signup, 201)
        login = other_api.request(
            "POST", "/auth/login",
            body={"email": identity["email"], "password": identity["password"]},
        )
        self.assert_status(login, 200)
        foreign_token = str(_data(login)["accessToken"])

        search_query = urllib.parse.urlencode({"q": "owner boundary", "mode": "KEYWORD"})
        probes = (
            ("library", "GET", f"/libraries/{library_id}"),
            ("references", "GET", f"/libraries/{library_id}/references"),
            ("pages", "GET", f"/libraries/{library_id}/pages"),
            ("ingest-jobs", "GET", f"/libraries/{library_id}/ingest-jobs"),
            ("change-sets", "GET", f"/libraries/{library_id}/change-sets"),
            ("search", "GET", f"/libraries/{library_id}/search?{search_query}"),
            ("conversations", "GET", f"/libraries/{library_id}/conversations"),
            ("sources", "GET", f"/sources?libraryId={library_id}"),
        )
        failures: list[str] = []
        for label, method, path in probes:
            response = self.api.request(method, path, token=foreign_token)
            if response.status not in BOUNDARY_STATUS:
                failures.append(f"{label}: status={response.status} body={response.text[:800]}")
                continue
            if library_id in response.text:
                failures.append(f"{label}: response leaked library id")
            if "M7 owner boundary" in response.text:
                failures.append(f"{label}: response leaked owner data")
        self.assertEqual([], failures, "M7 owner descendant boundary failures: " + repr(failures))


def _run_direct(argv: list[str]) -> int:
    suite = (
        unittest.defaultTestLoader.loadTestsFromTestCase(StaticM7SecurityAccessibilityTest)
        if "--self-check" in argv
        else unittest.defaultTestLoader.loadTestsFromModule(sys.modules[__name__])
    )
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    if result.skipped:
        for test, reason in result.skipped:
            print(f"QA_SKIP: {test} :: {reason}")
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(_run_direct(sys.argv[1:]))
