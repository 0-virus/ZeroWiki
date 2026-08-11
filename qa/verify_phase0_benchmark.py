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
CLASSIFICATION_LABELS = (
    "TRUE_CONFLICT",
    "TIME_CHANGE",
    "CONDITION_DIFF",
    "VIEWPOINT_DIFF",
    "POSSIBLE_CONFLICT",
    "NO_CONFLICT",
)
DENSE_STATUSES = {"COMPLETED", "FAILED"}
DENSE_MIN_COMPLETION_RATE = 1.0
DENSE_MIN_RECALL = 0.80
DENSE_MIN_ACCURACY = 0.80
DENSE_MAX_FPR = 0.10
DENSE_MAX_WILSON_FPR_UPPER = 0.20
WILSON_Z_95 = 1.959963984540054
GOLD_HEADERS = [
    "case_id",
    "claim_A_statement",
    "claim_B_statement",
    "classification",
    "판단_이유",
]
HOLDOUT_GOLD_HEADERS = ["case_id", "classification", "판단_이유"]
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
DENSE_PREDICTION_HEADERS = [
    "run_id",
    "prompt_variant",
    "case_id",
    "status",
    "prediction",
    "confidence",
    "expected_input_tokens",
    "expected_output_tokens",
    "expected_cost_usd",
    "expected_latency_seconds",
    "input_tokens",
    "output_tokens",
    "actual_cost_usd",
    "latency_seconds",
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


def nonnegative_integer(value: str, field: str, result: CheckResult, allow_blank: bool = False) -> int | None:
    parsed = nonnegative(value, field, result, allow_blank)
    if parsed is None:
        return None
    result.check(parsed.is_integer(), f"{field}: 정수 값")
    return int(parsed) if parsed.is_integer() else None


def wilson_upper_bound(successes: int, trials: int, z: float = WILSON_Z_95) -> float | None:
    if trials <= 0:
        return None
    successes = min(max(successes, 0), trials)
    rate = successes / trials
    denominator = 1 + (z * z / trials)
    center = rate + (z * z / (2 * trials))
    margin = z * math.sqrt((rate * (1 - rate) / trials) + (z * z / (4 * trials * trials)))
    return (center + margin) / denominator


def format_metric(value: float | None) -> str:
    return f"{value:.3f}" if value is not None else "N/A"


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


def validate_dense_predictions(
    rows: list[dict[str, str]],
    gold_rows: list[dict[str, str]],
    expected_run_id: str | None,
    expected_case_count: int | None,
    result: CheckResult,
    enforce_wilson_gate: bool = False,
) -> dict[str, Any]:
    """Validate the opt-in dense one-row-per-case prediction contract."""
    gold_case_ids = [row.get("case_id", "").strip() for row in gold_rows]
    expected_case_ids = set(gold_case_ids)
    result.check(bool(gold_rows), "dense gold labels: 행 존재")
    result.check(
        len(gold_case_ids) == len(expected_case_ids),
        "dense gold labels: case_id 유일성",
    )
    result.check(
        all(CASE_ID.fullmatch(case_id) for case_id in expected_case_ids),
        "dense gold labels: case_id 형식",
    )
    if expected_case_count is not None:
        result.check(
            len(expected_case_ids) == expected_case_count,
            f"dense gold labels: case 수 {len(expected_case_ids)} == expected {expected_case_count}",
        )

    run_ids = {row.get("run_id", "").strip() for row in rows}
    case_ids = [row.get("case_id", "").strip() for row in rows]
    result.check(len(run_ids) == 1 and all(run_ids), "dense predictions: 단일 run_id")
    if expected_run_id:
        result.check(run_ids == {expected_run_id}, "dense predictions: benchmark run_id 일치")
    result.check(len(case_ids) == len(set(case_ids)), "dense predictions: case_id 유일성")
    result.check(
        len(case_ids) == len(expected_case_ids),
        f"dense predictions: 행 수 {len(case_ids)} == expected cases {len(expected_case_ids)}",
    )
    result.check(
        set(case_ids) == expected_case_ids,
        "dense predictions: expected case_id 전체 대응",
    )

    predictions: dict[str, str | None] = {}
    statuses: dict[str, str] = {}
    for row in rows:
        case_id = row.get("case_id", "").strip()
        status = row.get("status", "").strip()
        prediction = row.get("prediction", "").strip()
        result.check(CASE_ID.fullmatch(case_id) is not None, f"dense {case_id}: case_id 형식")
        result.check(case_id in expected_case_ids, f"dense {case_id}: gold case_id 대응")
        result.check(status in DENSE_STATUSES, f"dense {case_id}: status enum")
        result.check(bool(row.get("prompt_variant", "").strip()), f"dense {case_id}: prompt_variant 필수값")
        nonnegative_integer(
            row.get("expected_input_tokens", ""),
            f"dense {case_id} expected_input_tokens",
            result,
        )
        nonnegative_integer(
            row.get("expected_output_tokens", ""),
            f"dense {case_id} expected_output_tokens",
            result,
        )
        nonnegative(row.get("expected_cost_usd", ""), f"dense {case_id} expected_cost_usd", result)
        nonnegative(
            row.get("expected_latency_seconds", ""),
            f"dense {case_id} expected_latency_seconds",
            result,
        )
        if case_id in statuses:
            continue
        statuses[case_id] = status
        if status == "COMPLETED":
            result.check(
                prediction in CLASSIFICATION_LABELS,
                f"dense {case_id}: prediction enum",
            )
            confidence = nonnegative(row.get("confidence", ""), f"dense {case_id} confidence", result)
            if confidence is not None:
                result.check(confidence <= 1, f"dense {case_id}: confidence <= 1")
            nonnegative_integer(row.get("input_tokens", ""), f"dense {case_id} input_tokens", result)
            nonnegative_integer(row.get("output_tokens", ""), f"dense {case_id} output_tokens", result)
            nonnegative(row.get("actual_cost_usd", ""), f"dense {case_id} actual_cost_usd", result)
            nonnegative(row.get("latency_seconds", ""), f"dense {case_id} latency_seconds", result)
            predictions[case_id] = prediction if prediction in CLASSIFICATION_LABELS else None
        elif status == "FAILED":
            result.check(
                not prediction,
                f"dense {case_id}: FAILED prediction은 비어 있어야 함",
            )
            for field in ("confidence", "input_tokens", "output_tokens", "actual_cost_usd", "latency_seconds"):
                result.check(
                    not row.get(field, "").strip(),
                    f"dense {case_id}: FAILED {field}는 비어 있어야 함",
                )
            # A failed call is unscored, never an implicit NO_CONFLICT prediction.
            predictions[case_id] = None

    expected_count = len(expected_case_ids)
    completed = sum(status == "COMPLETED" for status in statuses.values())
    failed = sum(status == "FAILED" for status in statuses.values())
    completion_rate = completed / expected_count if expected_count else 0.0
    conflicts = [row for row in gold_rows if row.get("classification") != NO_CONFLICT]
    negatives = [row for row in gold_rows if row.get("classification") == NO_CONFLICT]
    detected_conflicts = 0
    false_positives = 0
    correct = 0
    subtype_correct = 0
    matrix = {gold: {predicted: 0 for predicted in CLASSIFICATION_LABELS} for gold in CLASSIFICATION_LABELS}
    for row in gold_rows:
        case_id = row.get("case_id", "").strip()
        gold = row.get("classification", "").strip()
        predicted = predictions.get(case_id)
        if predicted in CLASSIFICATION_LABELS:
            matrix.setdefault(gold, {label: 0 for label in CLASSIFICATION_LABELS})
            if gold in matrix:
                matrix[gold][predicted] += 1
            if predicted == gold:
                correct += 1
        if gold != NO_CONFLICT:
            if predicted in CONTRADICTION_TYPES:
                detected_conflicts += 1
                if predicted == gold:
                    subtype_correct += 1
        elif predicted in CONTRADICTION_TYPES:
            false_positives += 1

    recall = detected_conflicts / len(conflicts) if conflicts else 0.0
    sampled_fpr = false_positives / len(negatives) if negatives else None
    accuracy = correct / len(gold_rows) if gold_rows else 0.0
    detected_subtype_accuracy = subtype_correct / detected_conflicts if detected_conflicts else None
    joint_subtype_accuracy = subtype_correct / len(conflicts) if conflicts else 0.0
    wilson_fpr_upper = wilson_upper_bound(false_positives, len(negatives))

    f1_scores: list[float] = []
    for label in CLASSIFICATION_LABELS:
        true_positive = matrix.get(label, {}).get(label, 0)
        predicted_total = sum(matrix.get(gold, {}).get(label, 0) for gold in CLASSIFICATION_LABELS)
        gold_total = sum(matrix.get(label, {}).values())
        precision = true_positive / predicted_total if predicted_total else 0.0
        class_recall = true_positive / gold_total if gold_total else 0.0
        f1_scores.append(
            2 * precision * class_recall / (precision + class_recall)
            if precision + class_recall
            else 0.0
        )
    macro_f1 = sum(f1_scores) / len(f1_scores)

    print(
        f"DENSE_METRICS expected={expected_count} completed={completed} failed={failed} "
        f"completion_rate={completion_rate:.3f} conflict={len(conflicts)} "
        f"detected={detected_conflicts} recall={recall:.3f} "
        f"sampled_fpr={format_metric(sampled_fpr)} "
        f"wilson95_fpr_upper={format_metric(wilson_fpr_upper)} "
        f"accuracy={accuracy:.3f} "
        f"detected_conflict_subtype_accuracy={format_metric(detected_subtype_accuracy)} "
        f"joint_subtype_accuracy={joint_subtype_accuracy:.3f} macro_f1={macro_f1:.3f}"
    )
    print(f"DENSE_CONFUSION labels={','.join(CLASSIFICATION_LABELS)}")
    for gold in CLASSIFICATION_LABELS:
        values = ",".join(str(matrix.get(gold, {}).get(predicted, 0)) for predicted in CLASSIFICATION_LABELS)
        print(f"DENSE_CONFUSION_ROW gold={gold} values={values}")

    result.check(
        completion_rate >= DENSE_MIN_COMPLETION_RATE,
        f"dense completion {completion_rate:.3f} >= {DENSE_MIN_COMPLETION_RATE:.3f}",
    )
    result.check(recall >= DENSE_MIN_RECALL, f"dense conflict recall {recall:.3f} >= {DENSE_MIN_RECALL:.3f}")
    result.check(
        accuracy >= DENSE_MIN_ACCURACY,
        f"dense 6-class accuracy {accuracy:.3f} >= {DENSE_MIN_ACCURACY:.3f}",
    )
    result.check(
        sampled_fpr is not None and sampled_fpr <= DENSE_MAX_FPR,
        f"dense sampled FPR {format_metric(sampled_fpr)} <= {DENSE_MAX_FPR:.3f}",
    )
    if enforce_wilson_gate:
        result.check(
            wilson_fpr_upper is not None and wilson_fpr_upper <= DENSE_MAX_WILSON_FPR_UPPER,
            f"dense Wilson 95% FPR upper {format_metric(wilson_fpr_upper)} <= {DENSE_MAX_WILSON_FPR_UPPER:.3f}",
        )
    else:
        result.passes.append(
            f"dense Wilson 95% FPR upper {format_metric(wilson_fpr_upper)} (reported; dev gate not enforced)"
        )
    return {
        "expected": expected_count,
        "completed": completed,
        "failed": failed,
        "completion_rate": completion_rate,
        "recall": recall,
        "sampled_fpr": sampled_fpr,
        "wilson95_fpr_upper": wilson_fpr_upper,
        "accuracy": accuracy,
        "detected_conflict_subtype_accuracy": detected_subtype_accuracy,
        "joint_subtype_accuracy": joint_subtype_accuracy,
        "macro_f1": macro_f1,
        "confusion": matrix,
    }


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

    dense_gold = [
        {"case_id": f"CT-{case_number:03d}", "classification": label}
        for case_number, label in enumerate(CLASSIFICATION_LABELS[:-1], start=1)
    ] + [
        {"case_id": f"CT-{case_number:03d}", "classification": NO_CONFLICT}
        for case_number in range(6, 26)
    ]
    dense_rows = [
        {
            "run_id": "dense-self-check",
            "prompt_variant": "self-check",
            "case_id": row["case_id"],
            "status": "COMPLETED",
            "prediction": row["classification"],
            "confidence": "1",
            "expected_input_tokens": "1",
            "expected_output_tokens": "2",
            "expected_cost_usd": "0.1",
            "expected_latency_seconds": "0.01",
            "input_tokens": "1",
            "output_tokens": "2",
            "actual_cost_usd": "0.1",
            "latency_seconds": "0.01",
        }
        for row in dense_gold
    ]
    dev_gold = dense_gold[:8]
    dev_quality = CheckResult()
    dev_metrics = validate_dense_predictions(
        dense_rows[:8], dev_gold, "dense-self-check", 8, dev_quality
    )
    assert not dev_quality.failures
    assert dev_metrics["wilson95_fpr_upper"] > DENSE_MAX_WILSON_FPR_UPPER
    dense_quality = CheckResult()
    dense_metrics = validate_dense_predictions(
        dense_rows, dense_gold, "dense-self-check", 25, dense_quality, enforce_wilson_gate=True
    )
    assert not dense_quality.failures
    assert dense_metrics["completion_rate"] == 1.0
    assert dense_metrics["recall"] == 1.0
    assert dense_metrics["sampled_fpr"] == 0.0
    assert dense_metrics["wilson95_fpr_upper"] <= DENSE_MAX_WILSON_FPR_UPPER
    assert dense_metrics["macro_f1"] == 1.0
    assert dense_metrics["confusion"]["TRUE_CONFLICT"]["TRUE_CONFLICT"] == 1

    missing_quality = CheckResult()
    missing_metrics = validate_dense_predictions(
        dense_rows[:-1], dense_gold, "dense-self-check", 25, missing_quality
    )
    assert any("expected case_id" in failure for failure in missing_quality.failures)
    assert missing_metrics["completion_rate"] < 1.0

    failed_rows = [row.copy() for row in dense_rows]
    failed_rows[0].update(
        {
            "status": "FAILED",
            "prediction": "",
            "confidence": "",
            "expected_input_tokens": "1",
            "expected_output_tokens": "2",
            "expected_cost_usd": "0.1",
            "expected_latency_seconds": "0.01",
            "input_tokens": "",
            "output_tokens": "",
            "actual_cost_usd": "",
            "latency_seconds": "",
        }
    )
    failed_quality = CheckResult()
    failed_metrics = validate_dense_predictions(
        failed_rows, dense_gold, "dense-self-check", 25, failed_quality
    )
    assert not any("FAILED prediction" in failure for failure in failed_quality.failures)
    assert failed_metrics["failed"] == 1
    assert failed_metrics["recall"] < 1.0
    assert failed_metrics["confusion"]["TRUE_CONFLICT"][NO_CONFLICT] == 0

    fpr_rows = [row.copy() for row in dense_rows]
    fpr_rows[5]["prediction"] = "TRUE_CONFLICT"
    fpr_quality = CheckResult()
    fpr_metrics = validate_dense_predictions(
        fpr_rows, dense_gold, "dense-self-check", 25, fpr_quality, enforce_wilson_gate=True
    )
    assert fpr_metrics["sampled_fpr"] == 0.05
    assert fpr_metrics["confusion"][NO_CONFLICT]["TRUE_CONFLICT"] == 1
    assert fpr_metrics["macro_f1"] < 1.0
    assert fpr_metrics["wilson95_fpr_upper"] > DENSE_MAX_WILSON_FPR_UPPER
    assert any("Wilson 95% FPR upper" in failure for failure in fpr_quality.failures)

    print(
        "SELF-CHECK PASS manifest, mapping, newline, sparse recall-only rejection, "
        "dense dev Wilson reporting/holdout gate, completeness/failure/FPR/confusion/macro-F1"
    )
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, help="backend schema v1 manifest JSON")
    parser.add_argument("--generated-claims", type=Path, help="backend generated-claims CSV")
    parser.add_argument("--benchmark-output", type=Path, help="backend per-document benchmark CSV")
    parser.add_argument("--contradiction-output", type=Path, help="optional contradiction output CSV for AC-5")
    parser.add_argument("--dense-predictions", type=Path, help="optional dense one-row-per-case predictions CSV")
    parser.add_argument("--labels", type=Path, default=Path("evaluation/contradiction-labels.csv"))
    parser.add_argument(
        "--holdout-labels",
        "--gold-labels",
        dest="holdout_labels",
        type=Path,
        help="optional dense holdout gold CSV (case_id,classification,판단_이유)",
    )
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
    dense_gold_rows = (
        load_csv(args.holdout_labels, HOLDOUT_GOLD_HEADERS, result)
        if args.holdout_labels is not None and args.dense_predictions is not None
        else gold_rows
    )
    manifest = load_json(args.manifest, result)
    pairs: dict[tuple[str, str], str] = {}
    if manifest is not None:
        validate_manifest(manifest, gold_rows, result)
        pairs = validate_generated_claims(load_csv(args.generated_claims, GENERATED_HEADERS, result), manifest, result)

    blocked = False
    benchmark_run_id = None
    dense_mode = args.dense_predictions is not None
    if (args.benchmark_output is None or args.expected_documents is None) and not dense_mode:
        print("BENCHMARK BLOCKED --benchmark-output와 --expected-documents가 필요합니다")
        blocked = True
    elif args.benchmark_output is not None and args.expected_documents is not None:
        benchmark_run_id = validate_benchmark_output(
            load_csv(args.benchmark_output, BENCHMARK_HEADERS, result),
            args.expected_documents,
            args.min_completion_rate,
            result,
        )

    manifest_run_id = manifest.get("run_id") if isinstance(manifest, dict) else None
    if dense_mode:
        if args.contradiction_output is not None:
            result.check(False, "dense/sparse contradiction inputs cannot be combined")
        expected_run_id = benchmark_run_id or (manifest_run_id if isinstance(manifest_run_id, str) else None)
        validate_dense_predictions(
            load_csv(args.dense_predictions, DENSE_PREDICTION_HEADERS, result),
            dense_gold_rows,
            expected_run_id,
            args.expected_documents,
            result,
            enforce_wilson_gate=args.holdout_labels is not None,
        )
    elif args.contradiction_output is None:
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
