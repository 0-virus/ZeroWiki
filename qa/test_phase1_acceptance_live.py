"""Phase 1 AC-1/AC-2/AC-4 sequential live acceptance.

This is intentionally one opt-in test.  It creates its own account/library,
keeps the 100-document archive in memory, and only uses the public API.  The
test is skipped during broad discovery unless ``ZEROWIKI_QA_RUN_PHASE1_ACCEPTANCE``
is truthy because it creates a real ingest/change-set fixture.

Run (PowerShell)::

    $env:ZEROWIKI_BASE_URL = "http://localhost:8080"
    $env:ZEROWIKI_QA_EXPECT_SECURE_COOKIE = "false"
    $env:ZEROWIKI_QA_RUN_PHASE1_ACCEPTANCE = "true"
    $env:ZEROWIKI_QA_POLL_SECONDS = "0.02"
    $env:ZEROWIKI_QA_PHASE1_TIMEOUT = "240"
    python -m unittest discover -s qa -p "test_phase1_acceptance_live.py" -v

No access token or signed upload URL is printed or persisted by this runner.
"""

from __future__ import annotations

import hashlib
import io
import os
import sys
import time
import unittest
import urllib.parse
import zipfile
from typing import Any

from test_m1_m2_blackbox import (
    LiveBlackBoxBase,
)
from test_m3_knowledge_blackbox import _assert_claim_evidence_trace
from test_m4_change_review_blackbox import _records as _change_records
from test_m6_search_ai_blackbox import (
    _citation_list,
    _records,
    _validate_citation_shape,
    _validate_search_results,
)


DOCUMENT_COUNT = 100
MINIMUM_COMPLETED = 95
POLL_SECONDS = float(os.environ.get("ZEROWIKI_QA_POLL_SECONDS", "0.5"))
TIMEOUT_SECONDS = float(os.environ.get("ZEROWIKI_QA_PHASE1_TIMEOUT", "240"))
TERMINAL_JOB_STATES = {"FAILED", "CANCELLED"}


def _truthy(name: str) -> bool:
    return os.environ.get(name, "").strip().lower() in {"1", "true", "yes", "on"}


def _phase1_archive() -> bytes:
    """Build 100 unique Markdown entries without touching the filesystem."""
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for number in range(1, DOCUMENT_COUNT + 1):
            keyword = f"phase1-unique-keyword-{number:03d}"
            body = (
                f"# Phase 1 Unique Topic {number:03d}\n\n"
                f"Unique search term: {keyword}.\n"
                f"This source records the acceptance fixture for unique topic {number:03d}.\n"
                f"Claim: {keyword} is the traceable fact for document {number:03d}.\n"
            )
            archive.writestr(f"Vault/Phase1-{number:03d}.md", body)
        archive.writestr("Vault/.obsidian/app.json", "{}")
    return stream.getvalue()


class Phase1AcceptanceLiveTest(LiveBlackBoxBase):
    """One sequential API-only fixture covering the Phase 1 knowledge gate."""

    def _status(self, response: Any, *allowed: int) -> None:
        if response.network_error:
            self.fail(f"API network error: {response.network_error}")
        if response.status not in allowed:
            code = None
            if isinstance(response.payload, dict):
                error = response.payload.get("error")
                if isinstance(error, dict):
                    code = error.get("code")
            self.fail(f"unexpected status={response.status}, errorCode={code}")

    def _payload_data(self, response: Any) -> Any:
        """Read an API envelope without putting response bodies in failures."""
        payload = response.payload
        if not isinstance(payload, dict) or "data" not in payload:
            self.fail("API response did not contain the documented data envelope")
        return payload["data"]

    def _job(self, token: str, library_id: str, job_id: str) -> dict[str, Any]:
        response = self.api.request(
            "GET", f"/libraries/{library_id}/ingest-jobs/{job_id}", token=token
        )
        self._status(response, 200)
        data = self._payload_data(response)
        self.assertIsInstance(data, dict)
        return data

    def _wait_job(
        self,
        token: str,
        library_id: str,
        job_id: str,
        *wanted: str,
    ) -> dict[str, Any]:
        deadline = time.monotonic() + TIMEOUT_SECONDS
        latest: dict[str, Any] = {}
        while True:
            latest = self._job(token, library_id, job_id)
            status = str(latest.get("status", "")).upper()
            if status in wanted:
                return latest
            if status in TERMINAL_JOB_STATES:
                self.fail(f"ingest entered terminal status={status}")
            if time.monotonic() >= deadline:
                self.fail(f"ingest timeout status={status}, timeoutSeconds={TIMEOUT_SECONDS}")
            time.sleep(POLL_SECONDS)

    def _source_versions(self, sources: list[dict[str, Any]]) -> tuple[list[str], dict[str, tuple[str, int]]]:
        ids: list[str] = []
        lookup: dict[str, tuple[str, int]] = {}
        for source in sources:
            source_id = source.get("id")
            latest = source.get("latestVersion")
            if not source_id or not isinstance(latest, dict) or not latest.get("id"):
                continue
            version_id = str(latest["id"])
            ids.append(version_id)
            if latest.get("versionNo") is not None:
                lookup[version_id] = (str(source_id), int(latest["versionNo"]))
        return ids, lookup

    def _wait_message(
        self,
        token: str,
        library_id: str,
        conversation_id: str,
        message_id: str,
    ) -> dict[str, Any]:
        deadline = time.monotonic() + TIMEOUT_SECONDS
        while True:
            response = self.api.request(
                "GET",
                f"/libraries/{library_id}/conversations/{conversation_id}/messages/{message_id}",
                token=token,
            )
            self._status(response, 200)
            data = self._payload_data(response)
            self.assertIsInstance(data, dict)
            status = str(data.get("status", "")).upper()
            if status == "COMPLETED":
                return data
            if status == "FAILED":
                self.fail("assistant message entered FAILED")
            if time.monotonic() >= deadline:
                self.fail(f"assistant message timeout status={status}")
            time.sleep(POLL_SECONDS)

    def _search(self, token: str, library_id: str, keyword: str, mode: str) -> list[dict[str, Any]]:
        query = urllib.parse.urlencode(
            {"q": keyword, "types": "PAGE,CLAIM,SOURCE", "mode": mode, "limit": "20"}
        )
        response = self.api.request("GET", f"/libraries/{library_id}/search?{query}", token=token)
        self._status(response, 200)
        data = self._payload_data(response)
        self.assertIsInstance(data, dict)
        self.assertEqual(mode, str(data.get("mode", "")).upper())
        results = _records(data, "results")
        self.assertTrue(results, f"search mode {mode} returned no fixture result")
        _validate_search_results(
            results,
            current_library_id=library_id,
            referenced_library_id=library_id,
            expected_scope="CURRENT",
        )
        return results

    def test_phase1_ac1_ac2_ac4_100_document_sequential_acceptance(self) -> None:
        if not _truthy("ZEROWIKI_QA_RUN_PHASE1_ACCEPTANCE"):
            self.skipTest(
                "OPT_IN_PHASE1_100_DOCS: set ZEROWIKI_QA_RUN_PHASE1_ACCEPTANCE=true "
                "for the sequential live acceptance"
            )

        _, token, _ = self.sign_up_and_login("phase1-acceptance")
        library_id, _ = self.create_library(token, "Phase 1 100-document acceptance")
        archive = _phase1_archive()
        with zipfile.ZipFile(io.BytesIO(archive)) as archive_reader:
            markdown_names = [
                name for name in archive_reader.namelist() if name.lower().endswith(".md")
            ]
        self.assertEqual(DOCUMENT_COUNT, len(markdown_names))
        self.assertEqual(DOCUMENT_COUNT, len(set(markdown_names)))

        upload = self.api.request(
            "POST",
            "/uploads",
            token=token,
            body={
                "fileName": "phase1-acceptance.zip",
                "mediaType": "application/zip",
                "byteSize": len(archive),
                "sha256": hashlib.sha256(archive).hexdigest(),
            },
        )
        self._status(upload, 200, 201)
        upload_data = self._payload_data(upload)
        self.assertIsInstance(upload_data, dict)
        upload_id = upload_data.get("uploadId")
        upload_url = upload_data.get("uploadUrl")
        required_headers = upload_data.get("requiredHeaders")
        self.assertTrue(upload_id)
        self.assertTrue(isinstance(upload_url, str) and urllib.parse.urlparse(upload_url).scheme in {"http", "https"})
        self.assertEqual("PUT", str(upload_data.get("method", "")).upper())
        self.assertIsInstance(required_headers, dict)
        self.assertTrue(upload_data.get("expiresAt"))

        put = self.api.request(
            "PUT",
            str(upload_url),
            raw_body=archive,
            headers={str(key): str(value) for key, value in required_headers.items()},
            content_type="application/zip",
            capture_cookies=False,
        )
        self._status(put, 200, 201, 204)

        complete = self.api.request(
            "POST",
            f"/uploads/{upload_id}/complete",
            token=token,
            body={
                "libraryId": library_id,
                "sourceType": "OBSIDIAN",
                "title": "Phase 1 acceptance source batch",
                "startIngest": True,
                "processingMode": "STANDARD",
            },
        )
        self._status(complete, 200, 201, 202)
        complete_data = self._payload_data(complete)
        self.assertIsInstance(complete_data, dict)
        sources = _records(complete_data, "sources")
        self.assertEqual(DOCUMENT_COUNT, len(sources))
        source_version_ids, source_lookup = self._source_versions(sources)
        self.assertEqual(DOCUMENT_COUNT, len(source_version_ids))
        self.assertEqual(DOCUMENT_COUNT, len(set(source_version_ids)))
        self.assertEqual(DOCUMENT_COUNT, len(source_lookup))
        ingest = complete_data.get("ingestJob")
        self.assertIsInstance(ingest, dict)
        job_id = ingest.get("id")
        self.assertTrue(job_id)
        job_id = str(job_id)

        plan_state = self._wait_job(token, library_id, job_id, "PLAN_REVIEW")
        plan = plan_state.get("plan") or {}
        approval_body: dict[str, Any] = {
            "decision": "APPROVE",
            "processingMode": "STANDARD",
        }
        if plan.get("estimatedTokens") is not None:
            approval_body["expectedEstimatedTokens"] = plan["estimatedTokens"]
        if plan_state.get("planRevision") is not None:
            approval_body["expectedPlanRevision"] = plan_state["planRevision"]
        approval = self.api.request(
            "POST",
            f"/libraries/{library_id}/ingest-jobs/{job_id}/plan-approval",
            token=token,
            body=approval_body,
        )
        self._status(approval, 200)

        change_review_state = self._wait_job(token, library_id, job_id, "CHANGE_REVIEW")
        items_response = self.api.request(
            "GET", f"/libraries/{library_id}/ingest-jobs/{job_id}/items", token=token
        )
        self._status(items_response, 200)
        items = _records(self._payload_data(items_response), "items")
        self.assertEqual(DOCUMENT_COUNT, len(items))
        done_count = sum(1 for item in items if str(item.get("status", "")).upper() == "DONE")
        self.assertGreaterEqual(done_count, MINIMUM_COMPLETED)
        self.assertEqual(DOCUMENT_COUNT, int((change_review_state.get("progress") or {}).get("total", DOCUMENT_COUNT)))

        change_sets_response = self.api.request(
            "GET", f"/libraries/{library_id}/change-sets?status=READY_FOR_REVIEW", token=token
        )
        self._status(change_sets_response, 200)
        change_sets = _change_records(self._payload_data(change_sets_response), "changeSets")
        self.assertTrue(change_sets)
        change_set_id = str(change_sets[0].get("id"))
        self.assertNotEqual("None", change_set_id)

        detail_response = self.api.request(
            "GET", f"/libraries/{library_id}/change-sets/{change_set_id}", token=token
        )
        self._status(detail_response, 200)
        detail = self._payload_data(detail_response)
        self.assertIsInstance(detail, dict)
        change_items = _change_records(detail, "items")
        self.assertTrue(change_items)
        item_ids = [str(item.get("id")) for item in change_items]
        self.assertTrue(all(item_id and item_id != "None" for item_id in item_ids))
        revision = int(detail.get("revision"))
        review = self.api.request(
            "POST",
            f"/libraries/{library_id}/change-sets/{change_set_id}/reviews",
            token=token,
            body={
                "expectedRevision": revision,
                "decisions": [
                    {"changeItemId": item_id, "decision": "APPROVE"}
                    for item_id in item_ids
                ],
            },
        )
        self._status(review, 200, 202)

        reviewed_response = self.api.request(
            "GET", f"/libraries/{library_id}/change-sets/{change_set_id}", token=token
        )
        self._status(reviewed_response, 200)
        reviewed = self._payload_data(reviewed_response)
        self.assertIsInstance(reviewed, dict)
        reviewed_items = _change_records(reviewed, "items")
        self.assertEqual(len(change_items), len(reviewed_items))
        self.assertTrue(all(str(item.get("reviewStatus", "")).upper() == "APPROVED" for item in reviewed_items))

        library_response = self.api.request("GET", f"/libraries/{library_id}", token=token)
        self._status(library_response, 200)
        library_data = self._payload_data(library_response)
        self.assertIsInstance(library_data, dict)
        current_version = library_data.get("currentVersion") or {}
        expected_library_version = int(current_version.get("versionNo", 0))
        apply = self.api.request(
            "POST",
            f"/libraries/{library_id}/change-sets/{change_set_id}/apply",
            token=token,
            body={
                "expectedChangeSetRevision": int(reviewed.get("revision")),
                "expectedLibraryVersion": expected_library_version,
                "summary": "Phase 1 100-document acceptance apply",
            },
        )
        self._status(apply, 200, 202)
        apply_data = self._payload_data(apply)
        self.assertIsInstance(apply_data, dict)
        self.assertEqual("APPLIED", str(apply_data.get("status", "")).upper())

        completed_state = self._wait_job(token, library_id, job_id, "COMPLETED")
        self.assertEqual("COMPLETED", str(completed_state.get("status")).upper())
        final_items_response = self.api.request(
            "GET", f"/libraries/{library_id}/ingest-jobs/{job_id}/items", token=token
        )
        self._status(final_items_response, 200)
        final_items = _records(self._payload_data(final_items_response), "items")
        final_done = sum(1 for item in final_items if str(item.get("status", "")).upper() == "DONE")
        self.assertGreaterEqual(final_done, MINIMUM_COMPLETED)

        pages_response = self.api.request(
            "GET",
            f"/libraries/{library_id}/pages?type=CONCEPT&status=PUBLISHED&limit=100",
            token=token,
        )
        self._status(pages_response, 200)
        pages = _records(self._payload_data(pages_response), "pages")
        self.assertGreaterEqual(len(pages), MINIMUM_COMPLETED)
        page_ids = {str(page.get("id")) for page in pages if page.get("id")}
        self.assertGreaterEqual(len(page_ids), MINIMUM_COMPLETED)

        home_response = self.api.request("GET", f"/libraries/{library_id}", token=token)
        self._status(home_response, 200)
        home_data = self._payload_data(home_response)
        self.assertIsInstance(home_data, dict)
        topics = _records((home_data.get("home") or {}).get("topics"), "topics")
        self.assertGreaterEqual(len(topics), 5)
        for topic in topics:
            self.assertTrue(topic.get("name"))
            self.assertIsInstance(topic.get("pageCount"), int)
        classified = sum(
            1
            for page in pages
            if str(page.get("pageType", "")).upper() == "CONCEPT"
            and str(page.get("status", "")).upper() == "PUBLISHED"
        )
        self.assertGreaterEqual(classified, MINIMUM_COMPLETED)

        first_page_id = next(iter(page_ids))
        claims_response = self.api.request(
            "GET",
            f"/libraries/{library_id}/pages/{first_page_id}/claims?include=evidences",
            token=token,
        )
        self._status(claims_response, 200)
        claims = _records(self._payload_data(claims_response), "claims")
        evidence_count = _assert_claim_evidence_trace(claims)
        self.assertGreater(evidence_count, 0)
        traced_source_ids = {
            str(evidence.get("sourceVersionId"))
            for claim in claims
            for evidence in claim.get("evidences", [])
        }
        self.assertTrue(traced_source_ids)
        self.assertTrue(traced_source_ids <= set(source_version_ids))
        self.assertTrue(traced_source_ids <= set(source_lookup))
        for source_version_id in traced_source_ids:
            source_id, version_no = source_lookup[source_version_id]
            content_response = self.api.request(
                "GET", f"/sources/{source_id}/versions/{version_no}/content", token=token
            )
            self._status(content_response, 200)

        keyword = "phase1-unique-keyword-001"
        for mode in ("KEYWORD", "SEMANTIC", "HYBRID"):
            self._search(token, library_id, keyword, mode)

        conversation_response = self.api.request(
            "POST",
            f"/libraries/{library_id}/conversations",
            token=token,
            body={"title": "Phase 1 acceptance conversation"},
        )
        self._status(conversation_response, 201)
        conversation = self._payload_data(conversation_response)
        self.assertIsInstance(conversation, dict)
        conversation_id = str(conversation.get("id"))
        self.assertNotEqual("None", conversation_id)
        ask = self.api.request(
            "POST",
            f"/libraries/{library_id}/conversations/{conversation_id}/messages",
            token=token,
            body={
                "content": f"Explain {keyword} and cite the supporting evidence.",
                "answerMode": "COMPARE",
                "processingMode": "STANDARD",
                "searchScope": {"currentLibrary": True, "referencedLibraryIds": []},
            },
        )
        self._status(ask, 200, 201, 202)
        queued = self._payload_data(ask)
        self.assertIsInstance(queued, dict)
        assistant_id = queued.get("assistantMessageId")
        self.assertTrue(assistant_id)
        answer = self._wait_message(token, library_id, conversation_id, str(assistant_id))
        self.assertEqual("ASSISTANT", str(answer.get("role", "")).upper())
        self.assertFalse(bool(answer.get("insufficientEvidence")))
        citations = _citation_list(answer)
        _validate_citation_shape(citations)
        citation_ids: list[str] = []
        for citation in citations:
            citation_ids.append(str(citation.get("id")))
            self.assertTrue(citation.get("id"))
            self.assertIn(str(citation.get("pageId")), page_ids)
            self.assertIn(str(citation.get("sourceVersionId")), set(source_version_ids))
            cited_claims_response = self.api.request(
                "GET",
                f"/libraries/{library_id}/pages/{citation['pageId']}/claims?include=evidences",
                token=token,
            )
            self._status(cited_claims_response, 200)
            cited_claims = _records(self._payload_data(cited_claims_response), "claims")
            matching = [claim for claim in cited_claims if str(claim.get("id")) == str(citation.get("claimId"))]
            self.assertTrue(matching)
            self.assertTrue(
                any(
                    str(evidence.get("sourceVersionId")) == str(citation.get("sourceVersionId"))
                    for evidence in matching[0].get("evidences", [])
                )
            )

        feedback = self.api.request(
            "POST",
            f"/libraries/{library_id}/messages/{assistant_id}/feedback",
            token=token,
            body={"rating": "HELPFUL", "comment": "Phase 1 acceptance"},
        )
        self._status(feedback, 200)
        feedback_data = self._payload_data(feedback)
        self.assertIsInstance(feedback_data, dict)
        self.assertIs(feedback_data.get("accepted"), True)

        proposal = self.api.request(
            "POST",
            f"/libraries/{library_id}/messages/{assistant_id}/save-proposal",
            token=token,
            body={
                "target": {
                    "type": "NEW_PAGE",
                    "pageType": "SYNTHESIS",
                    "title": "Phase 1 acceptance synthesis",
                },
                "includeCitationIds": citation_ids,
            },
        )
        self._status(proposal, 201)
        proposal_data = self._payload_data(proposal)
        self.assertIsInstance(proposal_data, dict)
        self.assertTrue(proposal_data.get("changeSetId"))
        print(
            "PHASE1_ACCEPTANCE_PASS "
            f"documents={DOCUMENT_COUNT} done={final_done} publishedConcepts={classified} "
            f"topics={len(topics)} citations={len(citations)}"
        )


def _run_direct(argv: list[str]) -> int:
    suite = unittest.defaultTestLoader.loadTestsFromModule(sys.modules[__name__])
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    if result.skipped:
        for test, reason in result.skipped:
            print(f"QA_SKIP: {test} :: {reason}")
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(_run_direct(sys.argv[1:]))
