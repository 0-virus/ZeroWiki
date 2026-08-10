#!/usr/bin/env python3
"""Phase 0 평가 CSV의 구조와 참조 무결성을 검증한다.

외부 패키지 없이 Python 표준 라이브러리만 사용한다.
"""

from __future__ import annotations

import csv
import io
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path


INVENTORY_HEADER = ["page_id", "page_slug", "title", "page_type", "file_path"]
RELATION_HEADER = [
    "source_page_slug",
    "target_page_slug",
    "relation_type",
    "direction",
    "confidence",
    "label",
    "근거_한줄",
]
CONTRADICTION_HEADER = [
    "case_id",
    "claim_A_statement",
    "claim_B_statement",
    "classification",
    "판단_이유",
]

PAGE_TYPES = {"source", "concept", "flow"}
RELATION_TYPES = {
    "RELATED_TO",
    "PART_OF",
    "DEPENDS_ON",
    "CAUSES",
    "CONTRASTS_WITH",
    "EXPLAINS",
    "APPLIES_TO",
}
DIRECTIONS = {"forward", "bidirectional"}
CONFIDENCES = {"high", "medium", "low"}
RELATION_LABELS = {"positive", "negative"}
CONTRADICTION_CLASSES = {
    "TRUE_CONFLICT",
    "TIME_CHANGE",
    "CONDITION_DIFF",
    "VIEWPOINT_DIFF",
    "POSSIBLE_CONFLICT",
    "NO_CONFLICT",
}


def read_csv(path: Path, expected_header: list[str], errors: list[str]) -> list[tuple[int, dict[str, str]]]:
    try:
        raw = path.read_bytes()
    except OSError as exc:
        errors.append(f"{path}: 읽기 실패: {exc}")
        return []

    if raw.startswith(b"\xef\xbb\xbf"):
        errors.append(f"{path}: UTF-8 BOM이 없어야 합니다")

    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError as exc:
        errors.append(f"{path}: UTF-8 디코딩 실패: {exc}")
        return []

    rows: list[tuple[int, dict[str, str]]] = []
    try:
        reader = csv.reader(io.StringIO(text, newline=""), strict=True)
        header = next(reader, None)
        if header != expected_header:
            errors.append(f"{path}: 헤더 불일치: {header!r} (기대: {expected_header!r})")
            return []

        for row in reader:
            line_number = reader.line_num
            if len(row) != len(expected_header):
                errors.append(
                    f"{path}:{line_number}: 열 수 {len(row)} (기대: {len(expected_header)})"
                )
                continue
            rows.append((line_number, dict(zip(expected_header, row, strict=True))))
    except csv.Error as exc:
        errors.append(f"{path}: CSV 파싱 실패: {exc}")

    return rows


def require_fields(
    path: Path,
    line_number: int,
    row: dict[str, str],
    fields: list[str],
    errors: list[str],
) -> None:
    empty = [field for field in fields if not row[field].strip()]
    if empty:
        errors.append(f"{path}:{line_number}: 빈 필드: {', '.join(empty)}")


def normalized_pair(left: str, right: str) -> tuple[str, str]:
    normalized = [
        " ".join(unicodedata.normalize("NFKC", value).split()).casefold()
        for value in (left, right)
    ]
    return tuple(sorted(normalized))  # type: ignore[return-value]


def validate_inventory(
    path: Path, rows: list[tuple[int, dict[str, str]]], errors: list[str]
) -> tuple[set[str], Counter[str]]:
    page_ids: dict[str, int] = {}
    slugs: dict[str, int] = {}
    page_type_counts: Counter[str] = Counter()

    for line_number, row in rows:
        require_fields(path, line_number, row, INVENTORY_HEADER, errors)

        page_id = row["page_id"].strip()
        slug = row["page_slug"].strip()
        page_type = row["page_type"].strip()

        if page_id in page_ids:
            errors.append(
                f"{path}:{line_number}: 중복 page_id {page_id!r} (최초 행 {page_ids[page_id]})"
            )
        else:
            page_ids[page_id] = line_number

        if slug in slugs:
            errors.append(
                f"{path}:{line_number}: 중복 page_slug {slug!r} (최초 행 {slugs[slug]})"
            )
        else:
            slugs[slug] = line_number

        if page_type not in PAGE_TYPES:
            errors.append(f"{path}:{line_number}: 허용되지 않은 page_type {page_type!r}")
        else:
            page_type_counts[page_type] += 1

    return set(slugs), page_type_counts


def validate_relations(
    path: Path,
    rows: list[tuple[int, dict[str, str]]],
    inventory_slugs: set[str],
    errors: list[str],
) -> tuple[Counter[str], Counter[str], Counter[str], Counter[str]]:
    pair_lines: dict[tuple[str, str], int] = {}
    label_counts: Counter[str] = Counter()
    type_counts: Counter[str] = Counter()
    direction_counts: Counter[str] = Counter()
    confidence_counts: Counter[str] = Counter()

    for line_number, row in rows:
        source = row["source_page_slug"].strip()
        target = row["target_page_slug"].strip()
        relation_type = row["relation_type"].strip()
        direction = row["direction"].strip()
        confidence = row["confidence"].strip()
        label = row["label"].strip()

        require_fields(
            path,
            line_number,
            row,
            ["source_page_slug", "target_page_slug", "confidence", "label", "근거_한줄"],
            errors,
        )

        for field, slug in (("source_page_slug", source), ("target_page_slug", target)):
            if slug and slug not in inventory_slugs:
                errors.append(f"{path}:{line_number}: {field}의 slug {slug!r}가 인벤토리에 없음")

        if source and target:
            if source == target:
                errors.append(f"{path}:{line_number}: 자기 자신을 가리키는 관계 쌍 {source!r}")
            pair = tuple(sorted((source, target)))
            if pair in pair_lines:
                errors.append(
                    f"{path}:{line_number}: 방향 무시 중복 쌍 {pair!r} (최초 행 {pair_lines[pair]})"
                )
            else:
                pair_lines[pair] = line_number

        if label not in RELATION_LABELS:
            errors.append(f"{path}:{line_number}: 허용되지 않은 label {label!r}")
        else:
            label_counts[label] += 1

        if confidence not in CONFIDENCES:
            errors.append(f"{path}:{line_number}: 허용되지 않은 confidence {confidence!r}")
        else:
            confidence_counts[confidence] += 1

        if label == "positive":
            if relation_type not in RELATION_TYPES:
                errors.append(
                    f"{path}:{line_number}: positive의 relation_type이 UD-16 7종이 아님: {relation_type!r}"
                )
            else:
                type_counts[relation_type] += 1
            if direction not in DIRECTIONS:
                errors.append(f"{path}:{line_number}: positive의 direction이 잘못됨: {direction!r}")
            else:
                direction_counts[direction] += 1
        elif label == "negative":
            if relation_type or direction:
                errors.append(
                    f"{path}:{line_number}: negative는 relation_type과 direction이 모두 비어야 함"
                )

    return label_counts, type_counts, direction_counts, confidence_counts


def validate_contradictions(
    path: Path, rows: list[tuple[int, dict[str, str]]], errors: list[str]
) -> tuple[Counter[str], int]:
    pair_lines: dict[tuple[str, str], int] = {}
    case_id_lines: dict[str, int] = {}
    class_counts: Counter[str] = Counter()

    for line_number, row in rows:
        require_fields(path, line_number, row, CONTRADICTION_HEADER, errors)
        case_id = row["case_id"].strip()
        claim_a = row["claim_A_statement"].strip()
        claim_b = row["claim_B_statement"].strip()
        classification = row["classification"].strip()

        if case_id:
            if not re.fullmatch(r"CT-\d{3}", case_id):
                errors.append(f"{path}:{line_number}: case_id 형식 오류 {case_id!r}")
            if case_id in case_id_lines:
                errors.append(
                    f"{path}:{line_number}: 중복 case_id {case_id!r} (최초 행 {case_id_lines[case_id]})"
                )
            else:
                case_id_lines[case_id] = line_number

        if claim_a and claim_b:
            pair = normalized_pair(claim_a, claim_b)
            if pair[0] == pair[1]:
                errors.append(f"{path}:{line_number}: 동일한 주장으로 구성된 쌍")
            if pair in pair_lines:
                errors.append(
                    f"{path}:{line_number}: 방향 무시 중복 주장 쌍 (최초 행 {pair_lines[pair]})"
                )
            else:
                pair_lines[pair] = line_number

        if classification not in CONTRADICTION_CLASSES:
            errors.append(f"{path}:{line_number}: 허용되지 않은 classification {classification!r}")
        else:
            class_counts[classification] += 1

    return class_counts, len(case_id_lines)


def format_counts(counts: Counter[str], order: list[str]) -> str:
    return ", ".join(f"{key}={counts[key]}" for key in order)


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")

    evaluation_dir = Path(__file__).resolve().parent
    inventory_path = evaluation_dir / "pages-inventory.csv"
    relation_path = evaluation_dir / "relation-labels.csv"
    contradiction_path = evaluation_dir / "contradiction-labels.csv"
    errors: list[str] = []

    inventory_rows = read_csv(inventory_path, INVENTORY_HEADER, errors)
    relation_rows = read_csv(relation_path, RELATION_HEADER, errors)
    contradiction_rows = read_csv(contradiction_path, CONTRADICTION_HEADER, errors)

    inventory_slugs, page_type_counts = validate_inventory(inventory_path, inventory_rows, errors)
    label_counts, type_counts, direction_counts, confidence_counts = validate_relations(
        relation_path, relation_rows, inventory_slugs, errors
    )
    class_counts, case_id_count = validate_contradictions(
        contradiction_path, contradiction_rows, errors
    )

    print(
        "pages-inventory.csv: "
        f"rows={len(inventory_rows)}, "
        + format_counts(page_type_counts, ["source", "concept", "flow"])
        + f", total={sum(page_type_counts.values())}"
    )
    print(
        "relation-labels.csv: "
        f"rows={len(relation_rows)}, "
        + format_counts(label_counts, ["positive", "negative"])
    )
    print("  relation_type: " + format_counts(type_counts, sorted(RELATION_TYPES)))
    print("  direction: " + format_counts(direction_counts, ["forward", "bidirectional"]))
    print("  confidence: " + format_counts(confidence_counts, ["high", "medium", "low"]))
    print(
        "contradiction-labels.csv: "
        f"rows={len(contradiction_rows)}, case_ids={case_id_count}, "
        + format_counts(
            class_counts,
            [
                "TRUE_CONFLICT",
                "TIME_CHANGE",
                "CONDITION_DIFF",
                "VIEWPOINT_DIFF",
                "POSSIBLE_CONFLICT",
                "NO_CONFLICT",
            ],
        )
    )

    if errors:
        print(f"검증 실패: 오류 {len(errors)}건", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print("검증 완료: 오류 0건")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
