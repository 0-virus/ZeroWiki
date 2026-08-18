"""ZeroWiki M4 change-review/apply/version black-box acceptance.

Only Python standard-library modules are used.  Static checks compare API
section 8 with the backend route inventory.  Live checks use a real page
fixture (``ZEROWIKI_QA_PAGE_ID`` or a page returned by the API), create an
edit proposal, and then verify owner isolation, immutable pre-apply versions,
optimistic locking, review, apply, and library version history.

The static route check composes the controller-level `/libraries/{libraryId}`
prefix with method-level Spring mappings, so annotation layout does not create
a false route failure.  Live checks still require a real page/version fixture.
"""

from __future__ import annotations

import os
import re
import sys
import unittest
from pathlib import Path
from typing import Any

from test_m1_m2_blackbox import (
    API_SPEC,
    ApiClient,
    BACKEND_SRC,
    LiveBlackBoxBase,
    _data,
    _id_from,
    _new_identity,
)


ROOT = Path(__file__).resolve().parents[1]
REQUIREMENTS = ROOT / "docs" / "ZeroWiki-요구사항-정의서.md"
SCENARIOS = ROOT / "qa" / "test_scenarios.md"
CHANGE_SET_CONTROLLER = (
    ROOT / "backend" / "src" / "main" / "java" / "com" / "zerowiki" /
    "knowledge" / "ChangeSetController.java"
)

CHANGE_SET_ENDPOINTS = (
    ("GET", "/libraries/{libraryId}/change-sets"),
    ("GET", "/libraries/{libraryId}/change-sets/{changeSetId}"),
    ("GET", "/libraries/{libraryId}/change-sets/{changeSetId}/items"),
    ("POST", "/libraries/{libraryId}/change-sets/{changeSetId}/reviews"),
    ("POST", "/libraries/{libraryId}/change-sets/{changeSetId}/apply"),
    ("POST", "/libraries/{libraryId}/change-sets/{changeSetId}/recalculate"),
)

LIBRARY_VERSION_ENDPOINTS = (
    ("GET", "/libraries/{libraryId}/versions"),
    ("GET", "/libraries/{libraryId}/versions/{versionNo}"),
    ("GET", "/libraries/{libraryId}/versions/diff"),
)


def _endpoint_pattern(method: str, path: str) -> str:
    return rf"\| `{re.escape(method)}` \| `{re.escape(path)}` \|"


def _records(value: Any, *keys: str) -> list[dict[str, Any]]:
    if isinstance(value, list):
        return [item for item in value if isinstance(item, dict)]
    if isinstance(value, dict):
        for key in (*keys, "items", "changeSets", "changeSetItems", "versions", "data", "results"):
            candidate = value.get(key)
            if isinstance(candidate, list):
                return [item for item in candidate if isinstance(item, dict)]
    return []


def _first_id(records: list[dict[str, Any]], *keys: str) -> str | None:
    for record in records:
        value = _id_from(record, keys or ("id",))
        if value:
            return value
    return None


class StaticChangeReviewContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        for path in (API_SPEC, REQUIREMENTS, SCENARIOS):
            if not path.is_file():
                raise AssertionError(f"missing M4 contract input: {path}")
        cls.api_text = API_SPEC.read_text(encoding="utf-8")
        cls.requirements_text = REQUIREMENTS.read_text(encoding="utf-8")
        cls.scenarios_text = SCENARIOS.read_text(encoding="utf-8")
        cls.change_set_controller_text = (
            CHANGE_SET_CONTROLLER.read_text(encoding="utf-8")
            if CHANGE_SET_CONTROLLER.is_file() else ""
        )
    def test_api_section_8_change_set_and_version_endpoints(self) -> None:
        missing = [
            f"{method} {path}"
            for method, path in (*CHANGE_SET_ENDPOINTS, *LIBRARY_VERSION_ENDPOINTS)
            if not re.search(_endpoint_pattern(method, path), self.api_text)
        ]
        self.assertEqual([], missing, f"API section 8 contract drift: {missing}")

    def test_change_review_lock_decision_and_apply_contract(self) -> None:
        required = (
            "revision",
            "expectedRevision",
            "expectedChangeSetRevision",
            "expectedLibraryVersion",
            "VERSION_CONFLICT",
            "READY_FOR_REVIEW",
            "APPROVE",
            "REJECT",
            "REQUEST_CHANGES",
            "DEFER",
            "comment",
            "changeSetId",
            "APPLIED",
            "FR-CHG-05",
            "FR-CHG-08",
            "FR-CHG-09",
            "FR-CHG-10",
            "FR-CHG-13",
        )
        source = self.api_text + self.requirements_text + self.scenarios_text
        missing = [term for term in required if term not in source]
        self.assertEqual([], missing, f"change review/version terms missing: {missing}")

    def test_backend_m4_route_inventory_is_present(self) -> None:
        self.assertTrue(
            self.change_set_controller_text,
            f"M4 controller source is missing: {CHANGE_SET_CONTROLLER}",
        )
        prefix = "/libraries/{libraryId}"
        self.assertRegex(
            self.change_set_controller_text,
            rf'@RequestMapping\(\s*"{re.escape(prefix)}"\s*\)',
            "M4 controller class-level route prefix is missing",
        )
        required_routes = (
            ("GET", "/change-sets"),
            ("GET", "/change-sets/{changeSetId}"),
            ("GET", "/change-sets/{changeSetId}/items"),
            ("POST", "/change-sets/{changeSetId}/reviews"),
            ("POST", "/change-sets/{changeSetId}/apply"),
            ("POST", "/change-sets/{changeSetId}/recalculate"),
            ("GET", "/versions"),
            ("GET", "/versions/diff"),
            ("GET", "/versions/{versionNo}"),
        )
        missing = []
        for method, suffix in required_routes:
            annotation = {"GET": "Get", "POST": "Post"}[method]
            pattern = rf'@{annotation}Mapping\(\s*"{re.escape(suffix)}"\s*\)'
            if not re.search(pattern, self.change_set_controller_text):
                missing.append(f"{method} {prefix}{suffix}")
        self.assertEqual([], missing, f"backend M4 route inventory missing: {missing}")


class M4LiveBlackBoxTest(LiveBlackBoxBase):
    def _context(self) -> tuple[str, str]:
        token = os.environ.get("ZEROWIKI_QA_ACCESS_TOKEN")
        library_id = os.environ.get("ZEROWIKI_QA_LIBRARY_ID")
        if token and library_id:
            return token, library_id
        _, token, _ = self.sign_up_and_login("m4")
        library_id, _ = self.create_library(token, "M4 change review")
        return token, library_id

    def _page(self, token: str, library_id: str) -> tuple[str, dict[str, Any]]:
        response = self.api.request(
            "GET", f"/libraries/{library_id}/pages?type=CONCEPT&status=PUBLISHED", token=token
        )
        self.assert_status(response, 200)
        pages = _records(_data(response), "pages")
        page_id = os.environ.get("ZEROWIKI_QA_PAGE_ID") or _first_id(pages, "id")
        if not page_id:
            self.skipTest("BLOCKED_M4_PAGE_FIXTURE: set ZEROWIKI_QA_PAGE_ID or seed a page")
        detail = self.api.request("GET", f"/libraries/{library_id}/pages/{page_id}", token=token)
        self.assert_status(detail, 200)
        page = _data(detail)
        self.assertIsInstance(page, dict)
        return str(page_id), page

    def _foreign_token(self) -> tuple[ApiClient, str]:
        foreign_api = ApiClient()
        identity = _new_identity("m4-foreign")
        signup = foreign_api.request("POST", "/auth/sign-up", body=identity)
        self.assert_status(signup, 201)
        login = foreign_api.request("POST", "/auth/login", body={
            "email": identity["email"],
            "password": identity["password"],
        })
        self.assert_status(login, 200)
        return foreign_api, str(_data(login)["accessToken"])

    def test_change_review_apply_version_and_owner_boundary(self) -> None:
        token, library_id = self._context()
        page_id, page = self._page(token, library_id)
        version = page.get("version")
        self.assertIsInstance(version, dict)
        base_version_no = int(version["versionNo"])
        base_markdown = str(version.get("markdownBody", ""))

        proposal = self.api.request(
            "POST", f"/libraries/{library_id}/pages/{page_id}/edit-proposals", token=token,
            body={
                "baseVersionNo": base_version_no,
                "proposedMarkdown": base_markdown + "\n\nM4 QA proposal",
                "reason": "M4 change review acceptance",
            },
        )
        self.assert_status(proposal, 201)
        proposal_data = _data(proposal)
        change_set_id = _id_from(proposal_data, ("changeSetId", "id"))
        self.assertTrue(change_set_id, f"proposal lacks changeSetId: {proposal.text[:800]}")

        before_versions = self.api.request(
            "GET", f"/libraries/{library_id}/pages/{page_id}/versions", token=token
        )
        self.assert_status(before_versions, 200)
        before = _records(_data(before_versions), "versions")

        change_sets = self.api.request(
            "GET", f"/libraries/{library_id}/change-sets?status=READY_FOR_REVIEW", token=token
        )
        self.assert_status(change_sets, 200)
        listed = _records(_data(change_sets), "changeSets")
        self.assertTrue(any(str(_id_from(item, ("id", "changeSetId"))) == str(change_set_id) for item in listed))

        detail = self.api.request(
            "GET", f"/libraries/{library_id}/change-sets/{change_set_id}", token=token
        )
        self.assert_status(detail, 200)
        change_set = _data(detail)
        self.assertEqual("READY_FOR_REVIEW", change_set.get("status"))
        revision = int(change_set["revision"])

        items_response = self.api.request(
            "GET", f"/libraries/{library_id}/change-sets/{change_set_id}/items", token=token
        )
        self.assert_status(items_response, 200)
        items = _records(_data(items_response), "items", "changeSetItems")
        self.assertTrue(items, "change set must expose reviewable items")
        item_ids = [str(_id_from(item, ("id", "changeItemId"))) for item in items]
        self.assertNotIn("None", item_ids)

        foreign_api, foreign_token = self._foreign_token()
        foreign_list = foreign_api.request(
            "GET", f"/libraries/{library_id}/change-sets", token=foreign_token
        )
        self.assert_status(foreign_list, 403, 404)
        self.assertNotIn(str(change_set_id), foreign_list.text)
        foreign_detail = foreign_api.request(
            "GET", f"/libraries/{library_id}/change-sets/{change_set_id}", token=foreign_token
        )
        self.assert_status(foreign_detail, 403, 404)
        self.assertNotIn(str(change_set_id), foreign_detail.text)

        decisions = [{"changeItemId": item_id, "decision": "APPROVE"} for item_id in item_ids]
        stale_review = self.api.request(
            "POST", f"/libraries/{library_id}/change-sets/{change_set_id}/reviews", token=token,
            body={"expectedRevision": max(0, revision - 1), "decisions": decisions},
        )
        self.assert_status(stale_review, 409)
        self.assertEqual("VERSION_CONFLICT", (stale_review.payload or {}).get("error", {}).get("code"))

        review = self.api.request(
            "POST", f"/libraries/{library_id}/change-sets/{change_set_id}/reviews", token=token,
            body={"expectedRevision": revision, "decisions": decisions},
        )
        self.assert_status(review, 200, 202)
        reviewed = self.api.request(
            "GET", f"/libraries/{library_id}/change-sets/{change_set_id}", token=token
        )
        self.assert_status(reviewed, 200)
        reviewed_data = _data(reviewed)
        apply_revision = int(reviewed_data["revision"])

        library = self.api.request("GET", f"/libraries/{library_id}", token=token)
        self.assert_status(library, 200)
        library_data = _data(library)
        current_version = library_data.get("currentVersion") or {}
        library_version = current_version.get("versionNo")
        if library_version is None:
            library_version = os.environ.get("ZEROWIKI_QA_LIBRARY_VERSION", "0")

        apply = self.api.request(
            "POST", f"/libraries/{library_id}/change-sets/{change_set_id}/apply", token=token,
            body={
                "expectedChangeSetRevision": apply_revision,
                "expectedLibraryVersion": int(library_version),
                "summary": "M4 QA apply",
            },
        )
        self.assert_status(apply, 200, 202)
        applied = _data(apply)
        self.assertEqual("APPLIED", applied.get("status"))

        after_versions = self.api.request(
            "GET", f"/libraries/{library_id}/pages/{page_id}/versions", token=token
        )
        self.assert_status(after_versions, 200)
        after = _records(_data(after_versions), "versions")
        self.assertTrue(any(int(item.get("versionNo", 0)) > base_version_no for item in after))

        library_versions = self.api.request(
            "GET", f"/libraries/{library_id}/versions", token=token
        )
        self.assert_status(library_versions, 200)
        self.assertTrue(_records(_data(library_versions), "versions"))

        # The proposal itself must not mutate the published snapshot; only
        # the reviewed apply may introduce a newer page/library version.
        self.assertEqual(base_markdown, before[0].get("markdownBody") if before else base_markdown)


def _run_direct(argv: list[str]) -> int:
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(StaticChangeReviewContractTest) \
        if "--self-check" in argv else unittest.defaultTestLoader.loadTestsFromModule(sys.modules[__name__])
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    if result.skipped:
        for test, reason in result.skipped:
            print(f"QA_SKIP: {test} :: {reason}")
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(_run_direct(sys.argv[1:]))
