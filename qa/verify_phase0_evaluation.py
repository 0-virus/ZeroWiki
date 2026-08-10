#!/usr/bin/env python3
"""Phase 0 evaluation CSV structural acceptance checks.

This verifier intentionally checks only deterministic dataset properties. Product
metric semantics (AC-3 user usefulness, output matching policy, precision/FPR
denominators) are reviewed in qa/phase0-evaluation-acceptance-report.md.
"""

from __future__ import annotations

import csv
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path


RELATION_TYPES = {
    "RELATED_TO",
    "PART_OF",
    "DEPENDS_ON",
    "CAUSES",
    "CONTRASTS_WITH",
    "EXPLAINS",
    "APPLIES_TO",
}
CONTRADICTION_TYPES = {
    "TRUE_CONFLICT",
    "TIME_CHANGE",
    "CONDITION_DIFF",
    "VIEWPOINT_DIFF",
    "POSSIBLE_CONFLICT",
}
NO_CONFLICT = "NO_CONFLICT"

PAGE_HEADERS = ["page_id", "page_slug", "title", "page_type", "file_path"]
RELATION_HEADERS = [
    "source_page_slug",
    "target_page_slug",
    "relation_type",
    "direction",
    "confidence",
    "label",
    "근거_한줄",
]
CONTRADICTION_HEADERS = [
    "case_id",
    "claim_A_statement",
    "claim_B_statement",
    "classification",
    "판단_이유",
]


class CheckResult:
    def __init__(self) -> None:
        self.failures: list[str] = []
        self.passes: list[str] = []

    def check(self, condition: bool, message: str) -> None:
        if condition:
            self.passes.append(message)
        else:
            self.failures.append(message)


def read_csv(path: Path, expected_headers: list[str], result: CheckResult) -> list[dict[str, str]]:
    raw = path.read_bytes()
    result.check(not raw.startswith(b"\xef\xbb\xbf"), f"{path.name}: UTF-8 BOM 없음")
    result.check(b"\r" not in raw, f"{path.name}: LF 줄바꿈")

    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError as exc:
        result.check(False, f"{path.name}: UTF-8 디코딩 ({exc})")
        return []

    try:
        reader = csv.DictReader(text.splitlines(), strict=True)
        rows = list(reader)
    except csv.Error as exc:
        result.check(False, f"{path.name}: CSV 파싱 ({exc})")
        return []

    result.check(reader.fieldnames == expected_headers, f"{path.name}: 정확한 헤더 스키마")
    result.check(all(None not in row for row in rows), f"{path.name}: 열 개수 일치")
    return rows


def normalized(value: str) -> str:
    return " ".join(unicodedata.normalize("NFKC", value).split()).casefold()


def duplicate_values(rows: list[dict[str, str]], key: str) -> list[str]:
    counts = Counter(normalized(row[key]) for row in rows)
    return sorted(value for value, count in counts.items() if count > 1)


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    evaluation = repo_root / "evaluation"
    result = CheckResult()

    pages = read_csv(evaluation / "pages-inventory.csv", PAGE_HEADERS, result)
    relations = read_csv(evaluation / "relation-labels.csv", RELATION_HEADERS, result)
    contradictions = read_csv(
        evaluation / "contradiction-labels.csv", CONTRADICTION_HEADERS, result
    )

    # Inventory: required values, stable identifiers, and one-to-one lookup keys.
    result.check(
        all(all(row[column].strip() for column in PAGE_HEADERS) for row in pages),
        "pages-inventory.csv: 필수값 존재",
    )
    result.check(
        all(re.fullmatch(r"page-\d{3}", row["page_id"]) for row in pages),
        "pages-inventory.csv: page_id 형식",
    )
    for key in ("page_id", "page_slug", "file_path"):
        result.check(
            not duplicate_values(pages, key),
            f"pages-inventory.csv: {key} 유일성",
        )
    page_types = Counter(row["page_type"] for row in pages)
    result.check(set(page_types) == {"concept", "flow", "source"}, "pages-inventory.csv: page_type enum")

    # Relation labels: endpoints exact-join the inventory page_slug values.
    slugs = {row["page_slug"] for row in pages}
    result.check(
        all(
            row["source_page_slug"] in slugs and row["target_page_slug"] in slugs
            for row in relations
        ),
        "relation-labels.csv: 모든 양 끝점이 inventory page_slug 참조",
    )
    result.check(
        all(row["source_page_slug"] != row["target_page_slug"] for row in relations),
        "relation-labels.csv: self relation 없음",
    )
    relation_pairs = [
        tuple(sorted((normalized(row["source_page_slug"]), normalized(row["target_page_slug"]))))
        for row in relations
    ]
    result.check(
        not [pair for pair, count in Counter(relation_pairs).items() if count > 1],
        "relation-labels.csv: 방향 무시 페이지 쌍 유일성",
    )
    result.check(
        all(
            row["source_page_slug"].strip()
            and row["target_page_slug"].strip()
            and row["confidence"].strip()
            and row["label"].strip()
            and row["근거_한줄"].strip()
            for row in relations
        ),
        "relation-labels.csv: 공통 필수값 존재",
    )
    result.check(
        all(row["label"] in {"positive", "negative"} for row in relations),
        "relation-labels.csv: label enum",
    )
    result.check(
        all(row["confidence"] in {"low", "medium", "high"} for row in relations),
        "relation-labels.csv: confidence enum",
    )
    positives = [row for row in relations if row["label"] == "positive"]
    negatives = [row for row in relations if row["label"] == "negative"]
    result.check(
        all(
            row["relation_type"] in RELATION_TYPES
            and row["direction"] in {"forward", "bidirectional"}
            for row in positives
        ),
        "relation-labels.csv: positive의 UD-16 relation_type/direction enum",
    )
    result.check(
        all(not row["relation_type"].strip() and not row["direction"].strip() for row in negatives),
        "relation-labels.csv: negative의 relation_type/direction 공란 규칙",
    )
    result.check(
        {row["relation_type"] for row in positives} == RELATION_TYPES,
        "relation-labels.csv: UD-16 7종 모두 포함",
    )
    result.check(bool(positives) and bool(negatives), "relation-labels.csv: positive/negative 분모 존재")

    # Contradiction labels: five conflict classes plus an explicit negative class.
    result.check(
        all(
            row["case_id"].strip()
            and
            row["claim_A_statement"].strip()
            and row["claim_B_statement"].strip()
            and row["classification"].strip()
            and row["판단_이유"].strip()
            for row in contradictions
        ),
        "contradiction-labels.csv: 필수값 존재",
    )
    case_ids = [row["case_id"] for row in contradictions]
    result.check(
        all(re.fullmatch(r"CT-\d{3}", case_id) for case_id in case_ids),
        "contradiction-labels.csv: case_id 형식",
    )
    result.check(
        len({normalized(case_id) for case_id in case_ids}) == len(case_ids),
        "contradiction-labels.csv: case_id 유일성",
    )
    expected_case_ids = [f"CT-{index:03d}" for index in range(1, len(contradictions) + 1)]
    result.check(
        case_ids == expected_case_ids,
        "contradiction-labels.csv: case_id 연속성(CT-001부터 행 순서대로)",
    )
    result.check(
        all(row["claim_A_statement"] != row["claim_B_statement"] for row in contradictions),
        "contradiction-labels.csv: 동일 문장 self pair 없음",
    )
    contradiction_pairs = [
        tuple(
            sorted(
                (
                    normalized(row["claim_A_statement"]),
                    normalized(row["claim_B_statement"]),
                )
            )
        )
        for row in contradictions
    ]
    result.check(
        not [pair for pair, count in Counter(contradiction_pairs).items() if count > 1],
        "contradiction-labels.csv: 방향 무시 문장 쌍 유일성",
    )
    classifications = Counter(row["classification"] for row in contradictions)
    result.check(
        set(classifications) == CONTRADICTION_TYPES | {NO_CONFLICT},
        "contradiction-labels.csv: 5종 + NO_CONFLICT enum",
    )
    result.check(
        all(classifications[classification] > 0 for classification in CONTRADICTION_TYPES),
        "contradiction-labels.csv: 모순 5종 모두 분모 존재",
    )
    contradiction_positive = sum(
        count for label, count in classifications.items() if label != NO_CONFLICT
    )
    contradiction_negative = classifications[NO_CONFLICT]
    result.check(
        contradiction_positive > 0 and contradiction_negative > 0,
        "contradiction-labels.csv: 탐지율/negative 분모 존재",
    )

    for message in result.passes:
        print(f"PASS {message}")
    for message in result.failures:
        print(f"FAIL {message}")

    print(
        "COUNTS "
        f"pages={len(pages)} "
        f"page_types={dict(sorted(page_types.items()))} "
        f"relations={len(relations)} positive={len(positives)} negative={len(negatives)} "
        f"contradictions={len(contradictions)} "
        f"conflict={contradiction_positive} no_conflict={contradiction_negative}"
    )
    print(f"RESULT {'FAIL' if result.failures else 'PASS'} failures={len(result.failures)}")
    return 1 if result.failures else 0


if __name__ == "__main__":
    sys.exit(main())
