#!/usr/bin/env python3
"""Independent acceptance checks for Phase 0 benchmark artifacts.

The manifest contract follows ``backend/scripts/phase0_benchmark.py``:
schema v1 JSON plus the generated-claims CSV.  The benchmark metrics CSV is
the backend plan's per-document output; AC-5 remains blocked until a
contradiction output is supplied and joined through the manifest.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import sys
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any

try:
    from verify_phase0_evaluation import CheckResult, read_csv, valid_newlines
except ModuleNotFoundError:  # pragma: no cover - supports module execution from repo root.
    from qa.verify_phase0_evaluation import CheckResult, read_csv, valid_newlines


CASE_ID = re.compile(r"CT-\d{3}\Z")
MANIFEST_SCHEMA_VERSION = 1
CONTRADICTION_TYPES = {
    "TRUE_CONFLICT",
    "TIME_CHANGE",
    "CONDITION_DIFF",
    "VIEWPOINT_DIFF",
    "POSSIBLE_CONFLICT",
}
NO_CONFLICT = "NO_CONFLICT"
GOLD_HEADERS = [
    "case_id",
    "claim_A_statement",
    "claim_B_statement",
    "classification",
    "판단_이유",
]
GENERATED_HEADERS = ["case_id", "claim_a_generated_id", "claim_b_generated_id"]
BENCHMARK_HEADERS = [
    "run_id",
    "document_id",
    "status",
    "scan_seconds",
    "process_seconds",
    "input_tokens",
    "output_tokens",
]
CONTRADICTION_OUTPUT_HEADERS = [
    "run_id",
    "claim_1_id",
    "claim_2_id",
    "classification",
    "confidence",
]


def load_csv(path: Path, headers: list[str], result: CheckResult) -> list[dict[str, str]]:
    if not path.is_file():
        result.check(False, f"{path}: 파일 없음")
        return []
    return read_csv(path, headers, result)


def load_json(path: Path, result: CheckResult) -> dict[str, Any] | None:
    if not path.is_file():
        result.check(False, f"{path}: 파일 없음")
        return None
    raw = path.read_bytes()
    result.check(not raw.startswith(b"\xef\xbb\xbf"), f"{path.name}: UTF-8 BOM 없음")
    result.check(valid_newlines(raw), f"{path.name}: LF 또는 CRLF 줄바꿈(혼합·bare CR 금지)")
    try:
        payload = json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        result.check(False, f"{path.name}: UTF-8/JSON 파싱 ({exc})")
        return None
    result.check(isinstance(payload, dict), f"{path.name}: JSON 객체")
    return payload if isinstance(payload, dict) else None


def nonnegative(value: str, field: str, result: CheckResult, allow_blank: bool = False) -> float | None:
    if not value.strip() and allow_blank:
        return None
    try:
        parsed = float(value)
    except ValueError:
        result.check(False, f"{field}: 숫자 아님 ({value!r})")
        return None
    result.check(math.isfinite(parsed) and parsed >= 0, f"{field}: 유한한 0 이상 값")
    return parsed


def validate_manifest(
    manifest: dict[str, Any], gold_rows: list[dict[str, str]], result: CheckResult
) -> dict[str, tuple[str, str]]:
    result.check(manifest.get("schema_version") == MANIFEST_SCHEMA_VERSION, "manifest: schema_version=1")
    generated_at = manifest.get("generated_at")
    try:
        parsed_generated_at = datetime.fromisoformat(generated_at) if isinstance(generated_at, str) else None
    except ValueError:
        parsed_generated_at = None
    result.check(
        parsed_generated_at is not None and parsed_generated_at.tzinfo is not None,
        "manifest: generated_at ISO-8601 timezone 값",
    )
    run_id = manifest.get("run_id")
    if run_id is not None:
        result.check(isinstance(run_id, str) and bool(run_id.strip()), "manifest: run_id 필수값")
    manifest_id = manifest.get("manifest_id")
    manifest_id_ok = (
        isinstance(manifest_id, str) and re.fullmatch(r"sha256:[0-9a-f]{64}", manifest_id) is not None
    )
    result.check(manifest_id_ok, "manifest: manifest_id sha256 형식")
    cases = manifest.get("cases")
    result.check(isinstance(cases, list), "manifest: cases 배열")
    if not isinstance(cases, list):
        return {}
    result.check(
        isinstance(manifest.get("case_count"), int)
        and not isinstance(manifest.get("case_count"), bool)
        and manifest.get("case_count") == len(cases),
        "manifest: case_count 일치",
    )
    if manifest_id_ok:
        canonical_payload: dict[str, Any] = {"schema_version": MANIFEST_SCHEMA_VERSION}
        if isinstance(run_id, str):
            canonical_payload["run_id"] = run_id
        canonical_payload["cases"] = cases
        expected_manifest_id = "sha256:" + hashlib.sha256(
            json.dumps(
                canonical_payload,
                ensure_ascii=False,
                sort_keys=True,
                separators=(",", ":"),
            ).encode("utf-8")
        ).hexdigest()
        result.check(manifest_id == expected_manifest_id, "manifest: cases/run_id 해시 무결성")

    gold_by_case = {
        row.get("case_id", "").strip(): row for row in gold_rows if row.get("case_id", "").strip()
    }
    result.check(bool(gold_by_case), "gold labels: 행 존재")
    gold_case_ids = set(gold_by_case)
    case_ids: list[str] = []
    for case in cases:
        if not isinstance(case, dict):
            result.check(False, "manifest: case 객체")
            continue
        case_id = case.get("case_id")
        case_ids.append(case_id.strip() if isinstance(case_id, str) else "")
        result.check(isinstance(case_id, str) and CASE_ID.fullmatch(case_id) is not None, "manifest: case_id 형식")
        gold = case.get("gold")
        result.check(isinstance(gold, dict), f"manifest {case_id}: gold 객체")
        if isinstance(gold, dict):
            label = gold_by_case.get(case_id.strip() if isinstance(case_id, str) else "")
            result.check(label is not None, f"manifest {case_id}: gold label 대응")
            for side in ("claim_a", "claim_b"):
                claim = gold.get(side)
                result.check(isinstance(claim, dict), f"manifest {case_id}: gold.{side} 객체")
                if isinstance(claim, dict):
                    result.check(
                        all(isinstance(claim.get(field), str) and claim[field].strip() for field in ("page_id", "page_slug", "claim_id")),
                        f"manifest {case_id}: gold.{side} page/claim 식별자",
                    )
            if label is not None:
                for field in ("classification", "claim_a_statement", "claim_b_statement"):
                    result.check(
                        gold.get(field) == label.get(
                            {
                                "claim_a_statement": "claim_A_statement",
                                "claim_b_statement": "claim_B_statement",
                            }.get(field, field)
                        ),
                        f"manifest {case_id}: gold.{field} 라벨 일치",
                    )
        generated = case.get("generated_claim_ids")
        result.check(isinstance(generated, list), f"manifest {case_id}: generated_claim_ids 배열")
        if isinstance(generated, list):
            result.check(all(isinstance(item, str) and item.strip() for item in generated), f"manifest {case_id}: generated claim ID 값")

    result.check(len(case_ids) == len(set(case_ids)), "manifest: case_id 유일성")
    result.check(set(case_ids) == gold_case_ids, "manifest: gold case_id 전체 대응")
    return {case_id: ("", "") for case_id in case_ids}


def validate_generated_claims(
    rows: list[dict[str, str]],
    manifest: dict[str, Any],
    result: CheckResult,
) -> dict[tuple[str, str], str]:
    cases = manifest.get("cases") if isinstance(manifest, dict) else []
    manifest_case_ids = {
        case.get("case_id") for case in cases if isinstance(case, dict) and isinstance(case.get("case_id"), str)
    }
    case_ids = [row["case_id"].strip() for row in rows]
    result.check(bool(rows), "generated claims: 행 존재")
    result.check(len(case_ids) == len(set(case_ids)), "generated claims: case_id 유일성")
    result.check(set(case_ids) == manifest_case_ids, "generated claims: manifest case_id 전체 대응")

    pairs: dict[tuple[str, str], str] = {}
    for row in rows:
        case_id = row["case_id"].strip()
        claim_a = row["claim_a_generated_id"].strip()
        claim_b = row["claim_b_generated_id"].strip()
        result.check(CASE_ID.fullmatch(case_id) is not None, f"generated claims {case_id}: case_id 형식")
        result.check(bool(claim_a and claim_b), f"generated claims {case_id}: claim ID 필수값")
        result.check(claim_a != claim_b, f"generated claims {case_id}: claim pair distinct")
        pair = tuple(sorted((claim_a, claim_b)))
        if pair[0] and pair[1]:
            result.check(pair not in pairs, f"generated claims {case_id}: claim pair unique")
            pairs[pair] = case_id

    by_case = {
        row["case_id"].strip(): tuple(sorted((row["claim_a_generated_id"].strip(), row["claim_b_generated_id"].strip())))
        for row in rows
    }
    for case in cases if isinstance(cases, list) else []:
        if not isinstance(case, dict):
            continue
        case_id = case.get("case_id")
        generated = case.get("generated_claim_ids")
        if isinstance(case_id, str) and isinstance(generated, list) and generated:
            result.check(
                len(generated) == 2 and tuple(sorted(generated)) == by_case.get(case_id),
                f"generated claims {case_id}: manifest generated_claim_ids 일치",
            )
    return pairs


def validate_benchmark_output(
    rows: list[dict[str, str]], expected_documents: int, minimum_completion_rate: float, result: CheckResult
) -> str | None:
    result.check(expected_documents > 0, "benchmark: expected document count > 0")
    result.check(0 <= minimum_completion_rate <= 1, "benchmark: completion threshold 0..1")
    result.check(bool(rows), "benchmark: 문서별 결과 행 존재")
    if not rows:
        return None
    run_ids = {row["run_id"].strip() for row in rows}
    document_ids = [row["document_id"].strip() for row in rows]
    result.check(len(run_ids) == 1 and all(run_ids), "benchmark: 단일 run_id")
    result.check(all(document_ids), "benchmark: document_id 필수값")
    result.check(len(set(document_ids)) == len(document_ids), "benchmark: document_id 유일성")
    result.check(len(rows) == expected_documents, f"benchmark: 행 수 {len(rows)} == expected {expected_documents}")
    statuses = Counter(row["status"].strip() for row in rows)
    result.check(set(statuses) <= {"COMPLETED", "FAILED"}, "benchmark: status enum")
    completed = statuses["COMPLETED"]
    rate = completed / expected_documents if expected_documents else 0
    result.check(rate >= minimum_completion_rate, f"benchmark: completion rate {rate:.3f} >= {minimum_completion_rate:.3f}")
    for row in rows:
        completed_row = row["status"].strip() == "COMPLETED"
        for field in ("scan_seconds", "process_seconds", "input_tokens", "output_tokens"):
            nonnegative(row[field], f"benchmark {row['document_id']} {field}", result, not completed_row)
    print(f"BENCHMARK_METRICS expected={expected_documents} completed={completed} completion_rate={rate:.3f}")
    return next(iter(run_ids), None) if len(run_ids) == 1 else None


def validate_contradiction_output(
    rows: list[dict[str, str]],
    generated_pairs: dict[tuple[str, str], str],
    gold_rows: list[dict[str, str]],
    run_id: str | None,
    minimum_recall: float,
    minimum_accuracy: float,
    result: CheckResult,
) -> None:
    run_ids = {row["run_id"].strip() for row in rows}
    result.check(len(run_ids) == 1 and all(run_ids), "contradiction output: 단일 run_id")
    if run_id:
        result.check(run_ids == {run_id}, "contradiction output: benchmark run_id 일치")
    detected: set[str] = set()
    predictions: dict[str, str] = {}
    for row in rows:
        pair = tuple(sorted((row["claim_1_id"].strip(), row["claim_2_id"].strip())))
        result.check(pair[0] != pair[1], "contradiction output: claim pair distinct")
        result.check(row["classification"] in CONTRADICTION_TYPES, "contradiction output: classification enum")
        confidence = nonnegative(row["confidence"], "contradiction output confidence", result)
        if confidence is not None:
            result.check(confidence <= 1, "contradiction output confidence <= 1")
        case_id = generated_pairs.get(pair)
        result.check(case_id is not None, "contradiction output: generated claim pair join")
        if case_id:
            result.check(case_id not in detected, f"contradiction output: case_id unique ({case_id})")
            detected.add(case_id)
            predictions[case_id] = row["classification"]
    conflicts = {row["case_id"].strip() for row in gold_rows if row["classification"] != NO_CONFLICT}
    negatives = {row["case_id"].strip() for row in gold_rows if row["classification"] == NO_CONFLICT}
    recall = len(detected & conflicts) / len(conflicts) if conflicts else 0
    fpr = len(detected & negatives) / len(negatives) if negatives else None
    accuracy = (
        sum(
            predictions.get(row["case_id"].strip(), NO_CONFLICT) == row["classification"]
            for row in gold_rows
        )
        / len(gold_rows)
        if gold_rows
        else 0
    )
    result.check(recall >= minimum_recall, f"AC-5 recall {recall:.3f} >= {minimum_recall:.3f}")
    result.check(
        accuracy >= minimum_accuracy,
        f"Phase 0 contradiction classification accuracy {accuracy:.3f} >= {minimum_accuracy:.3f}",
    )
    print(
        f"AC5_METRICS conflict={len(conflicts)} detected={len(detected & conflicts)} "
        f"recall={recall:.3f} sampled_fpr={fpr if fpr is not None else 'N/A'} "
        f"classification_accuracy={accuracy:.3f}"
    )


def self_check() -> int:
    assert valid_newlines(b"a,b\n1,2\n")
    assert valid_newlines(b"a,b\r\n1,2\r\n")
    assert not valid_newlines(b"a,b\r\n1,2\n")
    assert not valid_newlines(b"a,b\r1,2\r")
    sample = {
        "schema_version": 1,
        "generated_at": "2026-08-11T00:00:00+00:00",
        "run_id": "self-check",
        "case_count": 1,
        "cases": [{
            "case_id": "CT-001",
            "gold": {
                "claim_a": {"page_id": "page-a", "page_slug": "a", "claim_id": "gold-a"},
                "claim_b": {"page_id": "page-b", "page_slug": "b", "claim_id": "gold-b"},
                "classification": "TRUE_CONFLICT",
                "claim_a_statement": "A",
                "claim_b_statement": "B",
            },
            "generated_claim_ids": [],
        }],
    }
    canonical = json.dumps(
        {"schema_version": 1, "run_id": sample["run_id"], "cases": sample["cases"]},
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    sample["manifest_id"] = "sha256:" + hashlib.sha256(canonical).hexdigest()
    result = CheckResult()
    gold = [{
        "case_id": "CT-001",
        "classification": "TRUE_CONFLICT",
        "claim_A_statement": "A",
        "claim_B_statement": "B",
    }]
    validate_manifest(sample, gold, result)
    pairs = validate_generated_claims(
        [{"case_id": "CT-001", "claim_a_generated_id": "generated-a", "claim_b_generated_id": "generated-b"}],
        sample,
        result,
    )
    assert not result.failures and pairs == {("generated-a", "generated-b"): "CT-001"}
    quality = CheckResult()
    validate_contradiction_output(
        [{
            "run_id": "run-1",
            "claim_1_id": "generated-a",
            "claim_2_id": "generated-b",
            "classification": "CONDITION_DIFF",
            "confidence": "1",
        }],
        pairs,
        [{"case_id": "CT-001", "classification": "TRUE_CONFLICT"}],
        "run-1",
        0.8,
        0.8,
        quality,
    )
    assert any("classification accuracy" in failure for failure in quality.failures)
    print("SELF-CHECK PASS manifest, mapping, newline, recall-only false PASS rejection")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, help="backend schema v1 manifest JSON")
    parser.add_argument("--generated-claims", type=Path, help="backend generated-claims CSV")
    parser.add_argument("--benchmark-output", type=Path, help="backend per-document benchmark CSV")
    parser.add_argument("--contradiction-output", type=Path, help="optional contradiction output CSV for AC-5")
    parser.add_argument("--labels", type=Path, default=Path("evaluation/contradiction-labels.csv"))
    parser.add_argument("--expected-documents", type=int, help="expected benchmark document rows")
    parser.add_argument("--min-completion-rate", type=float, default=0.95)
    parser.add_argument("--min-contradiction-recall", type=float, default=0.80)
    parser.add_argument("--min-contradiction-accuracy", type=float, default=0.80)
    parser.add_argument("--self-check", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.self_check:
        return self_check()
    if not args.manifest or not args.generated_claims:
        print("BLOCKED --manifest와 --generated-claims가 필요합니다", file=sys.stderr)
        return 2

    result = CheckResult()
    gold_rows = load_csv(args.labels, GOLD_HEADERS, result)
    manifest = load_json(args.manifest, result)
    pairs: dict[tuple[str, str], str] = {}
    if manifest is not None:
        validate_manifest(manifest, gold_rows, result)
        pairs = validate_generated_claims(load_csv(args.generated_claims, GENERATED_HEADERS, result), manifest, result)

    blocked = False
    benchmark_run_id = None
    if args.benchmark_output is None or args.expected_documents is None:
        print("BENCHMARK BLOCKED --benchmark-output와 --expected-documents가 필요합니다")
        blocked = True
    else:
        benchmark_run_id = validate_benchmark_output(
            load_csv(args.benchmark_output, BENCHMARK_HEADERS, result),
            args.expected_documents,
            args.min_completion_rate,
            result,
        )

    if args.contradiction_output is None:
        print("AC5 BLOCKED --contradiction-output이 없어 Ingest-level 판정 불가")
        blocked = True
    else:
        validate_contradiction_output(
            load_csv(args.contradiction_output, CONTRADICTION_OUTPUT_HEADERS, result),
            pairs,
            gold_rows,
            benchmark_run_id,
            args.min_contradiction_recall,
            args.min_contradiction_accuracy,
            result,
        )

    for message in result.passes:
        print(f"PASS {message}")
    for message in result.failures:
        print(f"FAIL {message}")
    status = "FAIL" if result.failures else "BLOCKED" if blocked else "PASS"
    print(f"RESULT {status} failures={len(result.failures)}")
    return 1 if result.failures else 2 if blocked else 0


if __name__ == "__main__":
    raise SystemExit(main())
