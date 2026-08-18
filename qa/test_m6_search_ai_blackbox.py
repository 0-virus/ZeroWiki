"""ZeroWiki M6 search/AI stdlib black-box acceptance checks.

The static suite treats API section 9.1/9.2 as the contract and separately
checks the backend route inventory.  A missing backend surface is an explicit
FAIL, not a skipped product pass.  Live checks are bounded and use only the
documented API paths; fixture, provider, and server absence remain visible as
``BLOCKED_*`` results.

Examples (PowerShell)::

    python qa/test_m6_search_ai_blackbox.py --self-check
    python -m unittest discover -s qa -p "test_m6_search_ai_blackbox.py" -v

``UD-25`` latency is recorded for a completed answer.  One live sample is not
claimed to prove a p95 gate; set ``ZEROWIKI_QA_ENFORCE_LATENCY=true`` when a
prepared latency run should enforce the 20-second answer budget.
"""

from __future__ import annotations

import json
import os
import re
import sys
import time
import unittest
import urllib.parse
import uuid
from pathlib import Path
from typing import Any

from test_m1_m2_blackbox import (
    API_SPEC,
    ApiClient,
    BACKEND_SRC,
    LiveBlackBoxBase,
    Response,
    _data,
    _id_from,
    _library_body,
    _new_identity,
)


ROOT = Path(__file__).resolve().parents[1]
REQUIREMENTS = ROOT / "docs" / "ZeroWiki-요구사항-정의서.md"
SCENARIOS = ROOT / "qa" / "test_scenarios.md"

SEARCH_ENDPOINTS = (
    ("GET", "/libraries/{libraryId}/search"),
    ("POST", "/libraries/{libraryId}/search-expansions"),
)

AI_ENDPOINTS = (
    ("POST", "/libraries/{libraryId}/conversations"),
    ("GET", "/libraries/{libraryId}/conversations"),
    ("GET", "/libraries/{libraryId}/conversations/{conversationId}"),
    ("POST", "/libraries/{libraryId}/conversations/{conversationId}/messages"),
    ("GET", "/libraries/{libraryId}/conversations/{conversationId}/messages/{messageId}"),
    ("POST", "/libraries/{libraryId}/messages/{messageId}/save-proposal"),
    ("POST", "/libraries/{libraryId}/messages/{messageId}/feedback"),
)

SEARCH_MODES = ("KEYWORD", "SEMANTIC", "HYBRID")
SEARCH_SCOPES = ("CURRENT", "REFERENCED")
ALLOWED_BOUNDARY_STATUS = (403, 404, 422)
ANSWER_BUDGET_SECONDS = float(os.environ.get("ZEROWIKI_QA_ANSWER_LATENCY_BUDGET_SECONDS", "20"))
POLL_SECONDS = float(os.environ.get("ZEROWIKI_QA_POLL_SECONDS", "1"))
POLL_LIMIT_SECONDS = float(os.environ.get("ZEROWIKI_QA_POLL_LIMIT", "20"))


def _endpoint_pattern(method: str, path: str) -> str:
    return rf"\| `{re.escape(method)}` \| `{re.escape(path)}` \|"


def _records(value: Any, *keys: str) -> list[dict[str, Any]]:
    """Unwrap common list wrappers without deciding an undocumented schema."""
    if isinstance(value, list):
        return [item for item in value if isinstance(item, dict)]
    if isinstance(value, dict):
        for key in (*keys, "results", "items", "conversations", "messages", "data"):
            candidate = value.get(key)
            if isinstance(candidate, list):
                return [item for item in candidate if isinstance(item, dict)]
    return []


def _citation_list(value: Any) -> list[dict[str, Any]]:
    if isinstance(value, dict):
        direct = value.get("citations")
        if isinstance(direct, list):
            return [item for item in direct if isinstance(item, dict)]
        for nested in value.values():
            found = _citation_list(nested)
            if found:
                return found
    elif isinstance(value, list):
        for nested in value:
            found = _citation_list(nested)
            if found:
                return found
    return []


def _route_annotation(method: str) -> str:
    return {"GET": "Get", "POST": "Post", "PATCH": "Patch", "PUT": "Put", "DELETE": "Delete"}[method]


def _backend_route_present(method: str, path: str) -> bool:
    """Match full or controller-prefix + method-level Spring mappings.

    M4 exposed why a raw full-path search is too brittle: controllers may put
    ``/libraries/{libraryId}`` on the class and only a suffix on the method.
    This matcher recognizes that layout while still requiring method and path.
    """
    if not BACKEND_SRC.is_dir():
        return False
    mapping = _route_annotation(method)
    full_pattern = rf'@{mapping}Mapping\(\s*"{re.escape(path)}"\s*\)'
    prefix = "/libraries/{libraryId}"
    suffix = path[len(prefix):] if path.startswith(prefix) else None
    suffix_pattern = None
    if suffix is not None:
        suffix_pattern = rf'@{mapping}Mapping(?:\(\s*"{re.escape(suffix)}"\s*\)|\(\s*\))'
    for source_path in BACKEND_SRC.rglob("*.java"):
        text = source_path.read_text(encoding="utf-8")
        if re.search(full_pattern, text):
            return True
        if suffix_pattern and re.search(
            rf'@RequestMapping\(\s*"{re.escape(prefix)}"\s*\).*?{suffix_pattern}',
            text,
            flags=re.DOTALL,
        ):
            return True
    return False


def _validate_search_results(
    results: list[dict[str, Any]],
    *,
    current_library_id: str,
    referenced_library_id: str,
    expected_scope: str | None = None,
) -> None:
    """Validate the security-visible search result envelope."""
    if expected_scope is not None and expected_scope not in SEARCH_SCOPES:
        raise AssertionError(f"unknown expected search scope: {expected_scope}")
    for result in results:
        snippet = result.get("snippet")
        if not isinstance(snippet, str) or len(snippet) > 400:
            raise AssertionError(f"search snippet must be a string <=400 chars: {result}")
        origin = result.get("origin")
        if not isinstance(origin, dict):
            raise AssertionError(f"search result lacks origin envelope: {result}")
        scope = origin.get("scope")
        library_id = str(origin.get("libraryId", ""))
        if scope not in SEARCH_SCOPES:
            raise AssertionError(f"search result has invalid origin.scope: {result}")
        if library_id not in {current_library_id, referenced_library_id}:
            raise AssertionError(f"search result crossed library boundary: {result}")
        if expected_scope and scope != expected_scope:
            raise AssertionError(f"search result was not explicitly scoped {expected_scope}: {result}")
        if scope == "CURRENT" and library_id != current_library_id:
            raise AssertionError(f"CURRENT result points outside current library: {result}")
        if scope == "REFERENCED" and library_id != referenced_library_id:
            raise AssertionError(f"REFERENCED result points outside registered target: {result}")
        if not result.get("resultType") or not result.get("title"):
            raise AssertionError(f"search result identity is incomplete: {result}")
        if not isinstance(result.get("score"), (int, float)):
            raise AssertionError(f"search result score must be numeric: {result}")
        if not result.get("pageId") and not result.get("sourceId"):
            raise AssertionError(f"search result lacks pageId/sourceId: {result}")
        if not origin.get("libraryName"):
            raise AssertionError(f"search result origin lacks libraryName: {result}")


def _validate_citation_shape(citations: list[dict[str, Any]]) -> None:
    if not citations:
        raise AssertionError("completed answer with evidence must contain citations")
    for citation in citations:
        for key in ("pageId", "pageTitle", "claimId", "sourceVersionId", "locator"):
            if not citation.get(key):
                raise AssertionError(f"citation missing {key}: {citation}")
        if not isinstance(citation.get("locator"), (dict, str)):
            raise AssertionError(f"citation locator must be an object or string: {citation}")


class StaticSearchAiContractTest(unittest.TestCase):
    """API/requirements/scenario checks plus an explicit backend inventory gate."""

    @classmethod
    def setUpClass(cls) -> None:
        for path in (API_SPEC, REQUIREMENTS, SCENARIOS):
            if not path.is_file():
                raise AssertionError(f"missing M6 contract input: {path}")
        cls.api_text = API_SPEC.read_text(encoding="utf-8")
        cls.requirements_text = REQUIREMENTS.read_text(encoding="utf-8")
        cls.scenarios_text = SCENARIOS.read_text(encoding="utf-8")

    def test_api_9_1_and_9_2_endpoint_contract_is_present(self) -> None:
        missing = [
            f"{method} {path}"
            for method, path in (*SEARCH_ENDPOINTS, *AI_ENDPOINTS)
            if not re.search(_endpoint_pattern(method, path), self.api_text)
        ]
        self.assertEqual([], missing, f"M6 API §9.1~9.2 route contract drift: {missing}")

    def test_search_modes_scope_snippet_and_boundary_contract(self) -> None:
        required = (
            *SEARCH_MODES,
            *SEARCH_SCOPES,
            "origin",
            "libraryId",
            "libraryName",
            "snippet",
            "400자",
            "targetLibraryIds",
            "library_references",
            "FR-SCH-01",
            "FR-SCH-03",
            "FR-SCH-04",
            "FR-SCH-05",
            "FR-ASK-07",
            "NFR-SEC-01",
            "NFR-SEC-02",
            "NFR-SEC-03",
            "API 15절",
        )
        source = self.api_text + self.requirements_text
        missing = [term for term in required if term not in source]
        self.assertEqual([], missing, f"search/security contract terms missing: {missing}")

    def test_ai_poll_citation_insufficient_evidence_and_save_contract(self) -> None:
        required = (
            "searchScope",
            "currentLibrary",
            "referencedLibraryIds",
            "QUEUED",
            "COMPLETED",
            "insufficientEvidence",
            "citations",
            "pageId",
            "pageTitle",
            "claimId",
            "sourceVersionId",
            "locator",
            "missingEvidence",
            "expandToReferencedLibraries",
            "webSearchAvailable",
            "모른다는 내용을 답변에 명시",
            "save-proposal",
            "changeSetId",
            "FR-ASK-04",
            "FR-ASK-05",
            "FR-ASK-06",
            "FR-ASK-07",
            "FR-ASK-09",
            "FR-ASK-10",
            "FR-ASK-11",
            "답변 평가",
            "A-3",
        )
        source = self.api_text + self.requirements_text + self.scenarios_text
        missing = [term for term in required if term not in source]
        self.assertEqual([], missing, f"AI answer contract terms missing: {missing}")

    def test_ud25_answer_latency_is_recorded_not_falsely_claimed_as_p95(self) -> None:
        self.assertIn("UD-25", self.requirements_text)
        self.assertIn("p95", self.requirements_text)
        self.assertIn("20초", self.requirements_text)
        self.assertIn("M6_LATENCY_SECONDS", Path(__file__).read_text(encoding="utf-8"))

    def test_search_and_ai_static_helpers_enforce_boundary_and_trace_shape(self) -> None:
        current = "current-library"
        referenced = "referenced-library"
        _validate_search_results(
            [{
                "resultType": "PAGE",
                "title": "Raft",
                "snippet": "근거 발췌",
                "score": 0.9,
                "pageId": "page-1",
                "origin": {"scope": "CURRENT", "libraryId": current, "libraryName": "Current"},
            }],
            current_library_id=current,
            referenced_library_id=referenced,
            expected_scope="CURRENT",
        )
        with self.assertRaises(AssertionError):
            _validate_search_results(
                [{
                    "resultType": "PAGE",
                    "title": "leak",
                    "snippet": "x" * 401,
                    "score": 0.9,
                    "pageId": "page-leak",
                    "origin": {"scope": "REFERENCED", "libraryId": referenced, "libraryName": "Referenced"},
                }],
                current_library_id=current,
                referenced_library_id=referenced,
                expected_scope="CURRENT",
            )
        _validate_citation_shape([{
            "order": 1,
            "pageId": "page-1",
            "pageTitle": "Raft",
            "claimId": "claim-1",
            "sourceVersionId": "version-1",
            "locator": {"page": 2},
        }])

    def test_backend_m6_route_inventory_is_present(self) -> None:
        missing = [
            f"{method} {path}"
            for method, path in (*SEARCH_ENDPOINTS, *AI_ENDPOINTS)
            if not _backend_route_present(method, path)
        ]
        self.assertEqual(
            [],
            missing,
            "backend M6 route inventory drift: " + repr(missing),
        )


class M6LiveBlackBoxTest(LiveBlackBoxBase):
    """Live search/AI checks.  All fixture and provider gaps stay visible."""

    _context_cache: dict[str, Any] | None = None
    _answer_cache: dict[str, Any] | None = None

    def _login_external(self, client: ApiClient, label: str) -> str:
        identity = _new_identity(label)
        signup = client.request("POST", "/auth/sign-up", body=identity)
        self.assert_status(signup, 201)
        login = client.request(
            "POST",
            "/auth/login",
            body={"email": identity["email"], "password": identity["password"]},
        )
        self.assert_status(login, 200)
        token = _data(login).get("accessToken")
        self.assertTrue(token, f"login response lacks accessToken: {login.text[:800]}")
        return str(token)

    def _context(self) -> dict[str, Any]:
        if type(self)._context_cache is not None:
            return type(self)._context_cache

        owner_token = os.environ.get("ZEROWIKI_QA_ACCESS_TOKEN")
        current_library_id = os.environ.get("ZEROWIKI_QA_LIBRARY_ID")
        if not owner_token or not current_library_id:
            _, owner_token, _ = self.sign_up_and_login("m6-owner")
            current_library_id, _ = self.create_library(owner_token, "M6 search AI current")

        referenced_library_id = os.environ.get("ZEROWIKI_QA_REFERENCED_LIBRARY_ID")
        if not referenced_library_id:
            referenced_library_id, _ = self.create_library(owner_token, "M6 search AI referenced")

        references = self.api.request(
            "GET", f"/libraries/{current_library_id}/references", token=owner_token
        )
        self.assert_status(references, 200)
        registered = any(
            str(item.get("targetLibraryId")) == str(referenced_library_id)
            for item in _records(_data(references))
        )
        if not registered:
            add_reference = self.api.request(
                "POST", f"/libraries/{current_library_id}/references", token=owner_token,
                body={"targetLibraryId": referenced_library_id},
            )
            self.assert_status(add_reference, 201, 409)

        unregistered_library_id, _ = self.create_library(owner_token, "M6 search AI unregistered")

        foreign_api = ApiClient()
        foreign_token = self._login_external(foreign_api, "m6-foreign")
        foreign_library_response = foreign_api.request(
            "POST", "/libraries", token=foreign_token, body=_library_body("M6 foreign")
        )
        self.assert_status(foreign_library_response, 201)
        foreign_library_id = _id_from(_data(foreign_library_response))
        self.assertTrue(foreign_library_id)

        type(self)._context_cache = {
            "owner_token": str(owner_token),
            "current_library_id": str(current_library_id),
            "referenced_library_id": str(referenced_library_id),
            "unregistered_library_id": str(unregistered_library_id),
            "foreign_token": foreign_token,
            "foreign_library_id": str(foreign_library_id),
            "foreign_api": foreign_api,
        }
        return type(self)._context_cache

    def _search(self, token: str, library_id: str, mode: str) -> Response:
        query = urllib.parse.urlencode({
            "q": "raft leader election",
            "types": "PAGE,CLAIM,SOURCE",
            "mode": mode,
            "limit": "20",
        })
        return self.api.request("GET", f"/libraries/{library_id}/search?{query}", token=token)

    def test_search_modes_expansion_and_owner_reference_boundary(self) -> None:
        context = self._context()
        owner_token = context["owner_token"]
        current = context["current_library_id"]
        referenced = context["referenced_library_id"]
        all_current_results: list[dict[str, Any]] = []

        for mode in SEARCH_MODES:
            response = self._search(owner_token, current, mode)
            self.assert_status(response, 200)
            results = _records(_data(response), "results")
            _validate_search_results(
                results,
                current_library_id=current,
                referenced_library_id=referenced,
                expected_scope="CURRENT",
            )
            all_current_results.extend(results)

        expanded = self.api.request(
            "POST", f"/libraries/{current}/search-expansions", token=owner_token,
            body={
                "query": "Raft 장애 복구",
                "targetLibraryIds": [referenced],
                "mode": "HYBRID",
            },
        )
        self.assert_status(expanded, 200, 202)
        expanded_results = _records(_data(expanded), "results")
        _validate_search_results(
            expanded_results,
            current_library_id=current,
            referenced_library_id=referenced,
        )

        unregistered = self.api.request(
            "POST", f"/libraries/{current}/search-expansions", token=owner_token,
            body={
                "query": "unregistered target",
                "targetLibraryIds": [context["unregistered_library_id"]],
                "mode": "HYBRID",
            },
        )
        self.assert_status(unregistered, *ALLOWED_BOUNDARY_STATUS)
        foreign_target = self.api.request(
            "POST", f"/libraries/{current}/search-expansions", token=owner_token,
            body={
                "query": "foreign target",
                "targetLibraryIds": [context["foreign_library_id"]],
                "mode": "HYBRID",
            },
        )
        self.assert_status(foreign_target, *ALLOWED_BOUNDARY_STATUS)

        foreign_search = self._search(context["foreign_token"], current, "HYBRID")
        self.assert_status(foreign_search, *ALLOWED_BOUNDARY_STATUS)
        if foreign_search.payload is not None:
            leaked = _records(foreign_search.payload, "results")
            self.assertFalse(leaked, f"foreign owner received current-library search results: {leaked}")

        if not all_current_results and not expanded_results:
            self.skipTest(
                "BLOCKED_M6_SEARCH_FIXTURE: search routes accepted requests but no page/claim/source fixture returned"
            )

    def _create_and_poll_answer(self, context: dict[str, Any]) -> dict[str, Any]:
        if type(self)._answer_cache is not None:
            return type(self)._answer_cache
        token = context["owner_token"]
        library_id = context["current_library_id"]
        conversation = self.api.request(
            "POST", f"/libraries/{library_id}/conversations", token=token
        )
        self.assert_status(conversation, 200, 201)
        conversation_id = _id_from(_data(conversation), ("conversationId", "id"))
        self.assertTrue(conversation_id, f"conversation response lacks id: {conversation.text[:800]}")

        conversations = self.api.request(
            "GET", f"/libraries/{library_id}/conversations", token=token
        )
        self.assert_status(conversations, 200)
        detail = self.api.request(
            "GET", f"/libraries/{library_id}/conversations/{conversation_id}", token=token
        )
        self.assert_status(detail, 200)

        started_at = time.monotonic()
        question = self.api.request(
            "POST",
            f"/libraries/{library_id}/conversations/{conversation_id}/messages",
            token=token,
            body={
                "content": "Raft와 Paxos를 이해하기 쉬움과 운영 안정성 관점에서 비교해줘.",
                "answerMode": "COMPARE",
                "processingMode": "STANDARD",
                "searchScope": {"currentLibrary": True, "referencedLibraryIds": []},
            },
        )
        self.assert_status(question, 200, 201, 202)
        queued = _data(question)
        self.assertEqual("QUEUED", str(queued.get("status")).upper())
        assistant_id = _id_from(queued, ("assistantMessageId", "messageId", "id"))
        self.assertTrue(assistant_id, f"question response lacks assistantMessageId: {question.text[:800]}")

        deadline = time.monotonic() + POLL_LIMIT_SECONDS
        latest_response: Response | None = None
        latest_data: dict[str, Any] = {}
        while True:
            latest_response = self.api.request(
                "GET",
                f"/libraries/{library_id}/conversations/{conversation_id}/messages/{assistant_id}",
                token=token,
            )
            self.assert_status(latest_response, 200)
            raw_data = _data(latest_response)
            self.assertIsInstance(raw_data, dict)
            latest_data = raw_data
            status = str(latest_data.get("status", "")).upper()
            if status == "COMPLETED":
                break
            if status == "FAILED":
                self.fail(f"AI answer entered FAILED: {latest_response.text[:1000]}")
            if time.monotonic() >= deadline:
                self.fail(
                    f"BLOCKED_M6_ANSWER_TIMEOUT: status={status}, limit={POLL_LIMIT_SECONDS}s, "
                    f"body={latest_response.text[:1000]}"
                )
            delay = POLL_SECONDS
            if latest_response.headers is not None:
                retry_after = latest_response.headers.get("Retry-After")
                try:
                    if retry_after:
                        delay = max(0.0, float(retry_after))
                except (TypeError, ValueError):
                    pass
            time.sleep(delay)

        elapsed = time.monotonic() - started_at
        print(f"M6_LATENCY_SECONDS={elapsed:.3f} budget={ANSWER_BUDGET_SECONDS:.3f} sample=1")
        enforce = os.environ.get("ZEROWIKI_QA_ENFORCE_LATENCY", "").strip().lower() in {
            "1", "true", "yes", "on"
        }
        if enforce:
            self.assertLessEqual(
                elapsed,
                ANSWER_BUDGET_SECONDS,
                f"UD-25 answer sample exceeded configured budget: {elapsed:.3f}s",
            )

        self.assertEqual("ASSISTANT", str(latest_data.get("role", "")).upper())
        self.assertNotIn("changeSetId", latest_data, "answer was auto-saved before explicit proposal")
        insufficient = latest_data.get("insufficientEvidence")
        self.assertIsInstance(insufficient, bool)
        content = str(latest_data.get("content", ""))
        self.assertTrue(content)
        citations = _citation_list(latest_data)
        if insufficient:
            self.assertRegex(content, r"모른|알 수 없|근거가 부족")
            suggestions = latest_data.get("suggestions")
            self.assertIsInstance(suggestions, dict)
            for key in ("missingEvidence", "expandToReferencedLibraries", "webSearchAvailable"):
                self.assertIn(key, suggestions)
        else:
            _validate_citation_shape(citations)

        type(self)._answer_cache = {
            "token": token,
            "library_id": library_id,
            "conversation_id": str(conversation_id),
            "assistant_id": str(assistant_id),
            "answer": latest_data,
            "citations": citations,
            "insufficient": bool(insufficient),
        }
        return type(self)._answer_cache

    def _trace_citations(self, context: dict[str, Any], answer: dict[str, Any]) -> None:
        citations = answer["citations"]
        if answer["insufficient"]:
            if citations:
                _validate_citation_shape(citations)
            return
        _validate_citation_shape(citations)
        token = context["owner_token"]
        library_id = context["current_library_id"]
        source_versions: dict[str, tuple[str, int]] = {}
        sources = self.api.request("GET", "/sources", token=token)
        self.assert_status(sources, 200)
        for source in _records(_data(sources), "sources"):
            source_id = source.get("id")
            if not source_id:
                continue
            versions = self.api.request("GET", f"/sources/{source_id}/versions", token=token)
            self.assert_status(versions, 200)
            for version in _records(_data(versions), "versions"):
                if version.get("id") and version.get("versionNo") is not None:
                    source_versions[str(version["id"])] = (str(source_id), int(version["versionNo"]))

        for citation in citations:
            page_id = str(citation["pageId"])
            page = self.api.request("GET", f"/libraries/{library_id}/pages/{page_id}", token=token)
            self.assert_status(page, 200)
            page_data = _data(page)
            self.assertEqual(library_id, str(page_data.get("libraryId")))
            claims = self.api.request(
                "GET", f"/libraries/{library_id}/pages/{page_id}/claims?include=evidences", token=token
            )
            self.assert_status(claims, 200)
            claim_ids = {
                str(claim.get("id")) for claim in _records(_data(claims), "claims") if claim.get("id")
            }
            self.assertIn(str(citation["claimId"]), claim_ids)
            source_version_id = str(citation["sourceVersionId"])
            self.assertIn(source_version_id, source_versions)
            source_id, version_no = source_versions[source_version_id]
            content = self.api.request(
                "GET", f"/sources/{source_id}/versions/{version_no}/content", token=token
            )
            self.assert_status(content, 200)

    def test_conversation_poll_citation_trace_and_latency(self) -> None:
        context = self._context()
        answer = self._create_and_poll_answer(context)
        self._trace_citations(context, answer)

    def test_ai_question_idempotency_key_reuses_the_same_result(self) -> None:
        """API §2.5 probe: duplicate AI commands must not create two answers."""
        context = self._context()
        token = context["owner_token"]
        library_id = context["current_library_id"]
        conversation = self.api.request(
            "POST", f"/libraries/{library_id}/conversations", token=token
        )
        self.assert_status(conversation, 201)
        conversation_id = _id_from(_data(conversation), ("conversationId", "id"))
        self.assertTrue(conversation_id, f"conversation response lacks id: {conversation.text[:800]}")
        body = {
            "content": "멱등성 QA fixture의 동일 질문 결과를 반복 생성하지 말아야 한다.",
            "answerMode": "COMPARE",
            "processingMode": "STANDARD",
            "searchScope": {"currentLibrary": True, "referencedLibraryIds": []},
        }
        key = f"qa-m7-{uuid.uuid4().hex}"
        path = f"/libraries/{library_id}/conversations/{conversation_id}/messages"
        first = self.api.request(
            "POST", path, token=token, body=body, headers={"Idempotency-Key": key}
        )
        self.assert_status(first, 200, 201, 202)
        first_data = _data(first)
        first_id = _id_from(first_data, ("assistantMessageId", "messageId", "id"))
        self.assertTrue(first_id, f"first idempotent response lacks message id: {first.text[:800]}")

        second = self.api.request(
            "POST", path, token=token, body=body, headers={"Idempotency-Key": key}
        )
        if second.status == 409:
            code = (second.payload or {}).get("error", {}).get("code")
            self.assertEqual(
                "IDEMPOTENCY_KEY_IN_PROGRESS",
                code,
                "same body must return the stored result or only the documented in-progress conflict",
            )
            return
        self.assert_status(second, 200, 201, 202)
        second_id = _id_from(
            _data(second), ("assistantMessageId", "messageId", "id")
        )
        self.assertEqual(
            first_id,
            second_id,
            "same user/path/body/key created a second AI message; API §2.5 idempotency is missing",
        )

    def test_answer_save_proposal_feedback_and_owner_boundary(self) -> None:
        context = self._context()
        answer = self._create_and_poll_answer(context)
        token = context["owner_token"]
        library_id = context["current_library_id"]
        assistant_id = answer["assistant_id"]

        foreign_message = self.api.request(
            "GET",
            f"/libraries/{library_id}/conversations/{answer['conversation_id']}/messages/{assistant_id}",
            token=context["foreign_token"],
        )
        self.assert_status(foreign_message, *ALLOWED_BOUNDARY_STATUS[:2])
        self.assertNotIn(str(assistant_id), foreign_message.text)

        feedback_body: dict[str, Any] | None = None
        configured_feedback = os.environ.get("ZEROWIKI_QA_FEEDBACK_BODY")
        if configured_feedback:
            try:
                parsed = json.loads(configured_feedback)
            except json.JSONDecodeError as error:
                self.fail(f"ZEROWIKI_QA_FEEDBACK_BODY must be valid JSON: {error}")
            self.assertIsInstance(parsed, dict)
            feedback_body = parsed
        feedback_kwargs: dict[str, Any] = {"token": token}
        if feedback_body is not None:
            feedback_kwargs["body"] = feedback_body
        feedback = self.api.request(
            "POST", f"/libraries/{library_id}/messages/{assistant_id}/feedback", **feedback_kwargs
        )
        if feedback.status in (400, 415) and feedback_body is None:
            self.fail(
                "API §9.2 feedback request body is unspecified; endpoint rejected empty probe. "
                "Set ZEROWIKI_QA_FEEDBACK_BODY to the approved contract body."
            )
        self.assert_status(feedback, 200, 201, 202, 204)

        if answer["insufficient"]:
            self.skipTest("BLOCKED_M6_SAVE_PROPOSAL_FIXTURE: insufficient-evidence answer has no saveable citation")
        citation_ids = [str(item["id"]) for item in answer["citations"] if item.get("id")]
        configured_ids = os.environ.get("ZEROWIKI_QA_CITATION_IDS")
        if configured_ids:
            citation_ids = [item.strip() for item in configured_ids.split(",") if item.strip()]
        proposal = self.api.request(
            "POST", f"/libraries/{library_id}/messages/{assistant_id}/save-proposal", token=token,
            body={
                "target": {
                    "type": "NEW_PAGE",
                    "pageType": "SYNTHESIS",
                    "title": "Raft와 Paxos 비교",
                },
                "includeCitationIds": citation_ids,
            },
        )
        self.assert_status(proposal, 200, 201, 202)
        proposal_data = _data(proposal)
        self.assertTrue(
            _id_from(proposal_data, ("changeSetId", "id")),
            f"save-proposal response lacks changeSetId: {proposal.text[:800]}",
        )


def _run_direct(argv: list[str]) -> int:
    suite = (
        unittest.defaultTestLoader.loadTestsFromTestCase(StaticSearchAiContractTest)
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
