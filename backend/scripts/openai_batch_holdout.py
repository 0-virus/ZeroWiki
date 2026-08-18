#!/usr/bin/env python3
"""Run the approved 70-case contradiction holdout through OpenAI Batch.

Each invocation handles one tranche.  Re-run the same command to poll and
collect it.  Raw prompts and Batch output are kept in memory; only IDs,
aggregate usage, and the existing dense prediction CSV contract are stored.
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path
from typing import Any

import phase0_benchmark as bench


BATCH_PRICE = 0.5
REMAINING_CAP_USD = 0.24356
MAX_JSONL_LINES = 50_000
MAX_JSONL_BYTES = 200_000_000
COMPLETION_WINDOW = "24h"
TERMINAL = {"completed", "failed", "expired", "cancelled"}
PREDICTIONS = "openai-predictions.csv"


def request_json(api_key: str, method: str, url: str, body: bytes | None = None,
                 content_type: str = "application/json") -> dict[str, Any]:
    request = urllib.request.Request(
        url, data=body, method=method,
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": content_type},
    )
    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            value = json.load(response)
    except urllib.error.HTTPError as error:
        raise bench.HarnessError(f"OpenAI Batch HTTP 오류: {error.code}") from None
    except (OSError, urllib.error.URLError, json.JSONDecodeError) as error:
        raise bench.HarnessError(f"OpenAI Batch 호출 실패: {type(error).__name__}") from error
    if not isinstance(value, dict):
        raise bench.HarnessError("OpenAI Batch JSON 응답 형식 오류")
    return value


def download(api_key: str, url: str) -> bytes:
    request = urllib.request.Request(url, headers={"Authorization": f"Bearer {api_key}"})
    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            return response.read()
    except urllib.error.HTTPError as error:
        raise bench.HarnessError(f"OpenAI Batch file HTTP 오류: {error.code}") from None
    except (OSError, urllib.error.URLError) as error:
        raise bench.HarnessError(f"OpenAI Batch file 호출 실패: {type(error).__name__}") from error


def upload_jsonl(api_key: str, content: bytes) -> str:
    boundary = f"zerowiki-{uuid.uuid4().hex}"
    marker = boundary.encode("ascii")
    body = b"\r\n".join([
        b"--" + marker,
        b'Content-Disposition: form-data; name="purpose"', b"", b"batch",
        b"--" + marker,
        b'Content-Disposition: form-data; name="file"; filename="holdout.jsonl"',
        b"Content-Type: application/jsonl", b"", content,
        b"--" + marker + b"--", b"",
    ])
    value = request_json(
        api_key, "POST", bench.OPENAI_FILES_URL, body,
        f"multipart/form-data; boundary={boundary}",
    )
    file_id = value.get("id")
    if not isinstance(file_id, str) or not file_id:
        raise bench.HarnessError("OpenAI Batch input file id 누락")
    return file_id


def load_inputs(args: argparse.Namespace) -> tuple[list[dict[str, str]], list[dict[str, str]]]:
    inputs = bench.load_holdout_inputs(args.benchmark_input)
    gold = bench.load_gold_labels(args.gold_labels)
    joined, contexts = bench.join_holdout_gold(inputs, gold)
    # Prompt construction needs no gold fields.  The joined rows are retained
    # only for the existing manifest/mapping contract and later QA scoring.
    prompts: list[dict[str, str]] = []
    for row in inputs:
        prompt_row = dict(row)
        prompt = bench.build_openai_prompt(row, "rubric-context-v3", contexts[row["case_id"]])
        for forbidden in (
            row["case_id"], "classification", "판단_이유", "source_ref_A", "source_ref_B"
        ):
            if forbidden in prompt:
                raise bench.HarnessError(f"holdout prompt 금지 필드 누출: {forbidden}")
        prompt_row["prompt"] = prompt
        prompts.append(prompt_row)
    return joined, prompts


def specs(rows: list[dict[str, str]], max_output_tokens: int) -> list[dict[str, Any]]:
    values = []
    schema = bench.contradiction_schema()
    for row in rows:
        expected_input = bench.estimate_tokens(bench.OPENAI_SYSTEM_PROMPT + "\n" + row["prompt"])
        body = bench.build_openai_request_body(
            row["prompt"], schema, max_output_tokens=max_output_tokens
        )
        values.append({
            "case_id": row["case_id"], "body": body,
            "expected_input_tokens": expected_input,
            "expected_cost_usd": bench.openai_cost_usd(expected_input, max_output_tokens) * BATCH_PRICE,
        })
    return values


def jsonl(values: list[dict[str, Any]]) -> bytes:
    content = b"".join(
        (json.dumps({"custom_id": value["case_id"], "method": "POST",
                     "url": "/v1/responses", "body": value["body"]},
                    ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")
        for value in values
    )
    if len(values) > MAX_JSONL_LINES or len(content) > MAX_JSONL_BYTES:
        raise bench.HarnessError("OpenAI Batch JSONL 제한 초과")
    return content


def state_path(output: Path, offset: int, limit: int) -> Path:
    return output / f"batch-state-{offset:03d}-{offset + limit:03d}.json"


def write_state(path: Path, state: dict[str, Any]) -> None:
    path.write_text(json.dumps(state, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def existing_predictions(path: Path) -> list[dict[str, str]]:
    if not path.is_file():
        return []
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def response_value(body: dict[str, Any]) -> tuple[dict[str, Any], int, int]:
    usage = body.get("usage")
    if not isinstance(usage, dict):
        raise ValueError("usage missing")
    input_tokens = int(usage["input_tokens"])
    output_tokens = int(usage["output_tokens"])
    output_text = body.get("output_text")
    if not isinstance(output_text, str):
        output_text = None
        for item in body.get("output", []):
            if not isinstance(item, dict) or item.get("type") != "message":
                continue
            for content in item.get("content", []):
                if isinstance(content, dict) and content.get("type") == "output_text":
                    output_text = content.get("text")
                    break
    if not isinstance(output_text, str) or not output_text.strip():
        raise ValueError("output missing")
    value = json.loads(output_text)
    if not isinstance(value, dict) or value.get("classification") not in bench.CLASSIFICATIONS:
        raise ValueError("classification invalid")
    confidence = float(value.get("confidence"))
    if not 0 <= confidence <= 1:
        raise ValueError("confidence invalid")
    return value, input_tokens, output_tokens


def collect(api_key: str, args: argparse.Namespace, state: dict[str, Any],
            selected: list[dict[str, Any]], joined: list[dict[str, str]]) -> None:
    raw = b""
    if state.get("output_file_id"):
        raw = download(api_key, f"{bench.OPENAI_FILES_URL}/{state['output_file_id']}/content")
    outputs: dict[str, dict[str, Any]] = {}
    for line in raw.splitlines():
        try:
            item = json.loads(line)
        except json.JSONDecodeError:
            continue
        if isinstance(item, dict) and isinstance(item.get("custom_id"), str):
            outputs[item["custom_id"]] = item

    # Batch charges incomplete responses too.  Keep that aggregate in the
    # state/metrics ledger even though FAILED dense rows must leave usage blank.
    tranche_input = 0
    tranche_output = 0
    for item in outputs.values():
        response = item.get("response") if isinstance(item, dict) else None
        body = response.get("body") if isinstance(response, dict) else None
        usage = body.get("usage") if isinstance(body, dict) else None
        if isinstance(usage, dict):
            tranche_input += int(usage.get("input_tokens") or 0)
            tranche_output += int(usage.get("output_tokens") or 0)
    state.update({
        "actual_input_tokens": tranche_input,
        "actual_output_tokens": tranche_output,
        "actual_cost_usd": round(
            bench.openai_cost_usd(tranche_input, tranche_output) * BATCH_PRICE, 8
        ),
    })

    duration = max(0, int(state.get("completed_at", 0)) - int(state.get("created_at", 0)))
    new_rows = []
    for spec in selected:
        case_id = spec["case_id"]
        item = outputs.get(case_id, {})
        response = item.get("response") if isinstance(item, dict) else None
        response_body = response.get("body") if isinstance(response, dict) and response.get("status_code") == 200 else None
        row = {
            "run_id": args.run_id, "prompt_variant": "rubric-context-v3-batch",
            "case_id": case_id, "status": "FAILED", "prediction": "", "confidence": "",
            "expected_input_tokens": spec["expected_input_tokens"],
            "expected_output_tokens": args.max_output_tokens,
            "expected_cost_usd": f"{spec['expected_cost_usd']:.8f}",
            "expected_latency_seconds": "86400.000", "input_tokens": "", "output_tokens": "",
            "actual_cost_usd": "", "latency_seconds": "",
        }
        try:
            if not isinstance(response_body, dict):
                raise ValueError("request failed")
            value, input_tokens, output_tokens = response_value(response_body)
            row.update({
                "status": "COMPLETED", "prediction": value["classification"],
                "confidence": f"{float(value['confidence']):.6f}",
                "input_tokens": input_tokens, "output_tokens": output_tokens,
                "actual_cost_usd": f"{bench.openai_cost_usd(input_tokens, output_tokens) * BATCH_PRICE:.8f}",
                "latency_seconds": f"{duration:.6f}",
            })
        except (KeyError, TypeError, ValueError, json.JSONDecodeError):
            pass
        new_rows.append(row)

    prediction_path = args.output_dir / PREDICTIONS
    merged = {row["case_id"]: row for row in existing_predictions(prediction_path)}
    merged.update({row["case_id"]: row for row in new_rows})
    ordered = [merged[key] for key in sorted(merged)]
    bench.write_csv(prediction_path, bench.OPENAI_PREDICTION_HEADER, ordered)

    manifest, generated = bench.phase0_manifest(joined, args.run_id)
    manifest.update({
        "benchmark_kind": "contradiction_holdout", "prompt_variant": "rubric-context-v3-batch",
        "provider": "openai-batch", "prediction_rows": len(ordered),
    })
    (args.output_dir / "phase0-manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    bench.write_csv(args.output_dir / "phase0-generated-claims.csv", bench.GENERATED_HEADER, generated)
    benchmark_rows = [{
        "run_id": row["run_id"], "document_id": row["case_id"], "status": row["status"],
        "scan_seconds": "0.000000" if row["status"] == "COMPLETED" else "",
        "process_seconds": row["latency_seconds"], "input_tokens": row["input_tokens"],
        "output_tokens": row["output_tokens"],
    } for row in ordered]
    bench.write_csv(args.output_dir / "phase0-benchmark.csv", bench.BENCHMARK_HEADER, benchmark_rows)
    collected_states = []
    for path in args.output_dir.glob("batch-state-*.json"):
        value = json.loads(path.read_text(encoding="utf-8"))
        if value.get("collected") or value.get("output_file_id") == state.get("output_file_id"):
            collected_states.append(value if value.get("output_file_id") != state.get("output_file_id") else state)
    metrics = {
        "run_id": args.run_id, "provider": "openai-batch", "model": bench.OPENAI_BENCHMARK_MODEL,
        "completed": sum(row["status"] == "COMPLETED" for row in ordered),
        "failed": sum(row["status"] == "FAILED" for row in ordered),
        "input_tokens": sum(int(value.get("actual_input_tokens") or 0) for value in collected_states),
        "output_tokens": sum(int(value.get("actual_output_tokens") or 0) for value in collected_states),
        "actual_cost_usd": round(sum(float(value.get("actual_cost_usd") or 0) for value in collected_states), 8),
        "max_cost_usd": args.max_cost_usd,
        "batch_count": len(collected_states),
    }
    (args.output_dir / "phase0-batch-metrics.json").write_text(
        json.dumps(metrics, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--benchmark-input", type=Path, default=Path("evaluation/contradiction-holdout-input.csv"))
    parser.add_argument("--gold-labels", type=Path, default=Path("qa/contradiction-holdout-gold.csv"))
    parser.add_argument("--dotenv", type=Path, default=Path(".env"))
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--run-id", default="phase1-holdout-gpt-5_6-sol-batch-r1")
    parser.add_argument("--offset", type=int, default=0)
    parser.add_argument("--limit", type=int, default=20)
    parser.add_argument("--max-output-tokens", type=int, default=bench.OPENAI_MAX_OUTPUT_TOKENS)
    parser.add_argument("--max-cost-usd", type=float, default=0.24356)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--self-check", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.max_output_tokens < 1:
        raise bench.HarnessError("max-output-tokens must be at least 1")
    if not 0 < args.max_cost_usd <= REMAINING_CAP_USD:
        raise bench.HarnessError(
            f"max-cost-usd must be in (0, {REMAINING_CAP_USD:.5f}]"
        )
    joined, prompt_rows = load_inputs(args)
    if args.offset < 0 or args.limit < 1 or args.offset + args.limit > len(prompt_rows):
        raise bench.HarnessError("offset/limit 범위 오류")
    selected = specs(prompt_rows[args.offset:args.offset + args.limit], args.max_output_tokens)
    content = jsonl(selected)
    reservation = sum(value["expected_cost_usd"] for value in selected)
    if args.self_check or args.dry_run:
        parsed = [json.loads(line) for line in content.splitlines()]
        expected_ids = {
            row["case_id"] for row in prompt_rows[args.offset:args.offset + args.limit]
        }
        assert len(parsed) == args.limit
        assert {row["custom_id"] for row in parsed} == expected_ids
        for row in parsed:
            body = row["body"]
            assert row["method"] == "POST" and row["url"] == "/v1/responses"
            assert body["model"] == bench.OPENAI_BENCHMARK_MODEL
            assert body["store"] is False
            assert body["max_output_tokens"] == args.max_output_tokens
            format_spec = body["text"]["format"]
            assert format_spec["type"] == "json_schema"
            assert format_spec["name"] == "contradiction_classification"
            assert format_spec["strict"] is True
            assert format_spec["schema"] == bench.contradiction_schema()
            prompt_text = json.dumps(body["input"], ensure_ascii=False)
            assert row["custom_id"] not in prompt_text
            assert all(field not in prompt_text for field in (
                "classification", "판단_이유", "source_ref_A", "source_ref_B"
            ))
        assert abs(BATCH_PRICE - 0.5) < 1e-12
        if reservation > args.max_cost_usd + 1e-12:
            raise bench.HarnessError(
                f"Batch hard cap 초과: reservation={reservation:.8f} max={args.max_cost_usd:.8f}"
            )
        print(f"BATCH_PREFLIGHT cases={args.limit} bytes={len(content)} reservation_usd={reservation:.8f}")
        return 0

    args.output_dir.mkdir(parents=True, exist_ok=True)
    prediction_path = args.output_dir / PREDICTIONS
    actual_spend = 0.0
    active_reservations = 0.0
    for prior_state_path in args.output_dir.glob("batch-state-*.json"):
        prior_state = json.loads(prior_state_path.read_text(encoding="utf-8"))
        actual_spend += float(prior_state.get("actual_cost_usd") or 0)
        if not prior_state.get("actual_cost_usd"):
            active_reservations += float(prior_state.get("reservation_usd") or 0)
    api_key = bench.load_dotenv_api_key(args.dotenv)
    path = state_path(args.output_dir, args.offset, args.limit)
    if not path.is_file():
        remaining = args.max_cost_usd - actual_spend - active_reservations
        if reservation > remaining + 1e-12:
            raise bench.HarnessError(
                f"Batch hard cap 초과: reservation={reservation:.8f} remaining={remaining:.8f}"
            )
        input_file_id = upload_jsonl(api_key, content)
        created = request_json(api_key, "POST", bench.OPENAI_BATCHES_URL, json.dumps({
            "input_file_id": input_file_id, "endpoint": "/v1/responses",
            "completion_window": COMPLETION_WINDOW, "metadata": {"run_id": args.run_id,
            "tranche": f"{args.offset}-{args.offset + args.limit}"},
        }).encode("utf-8"))
        batch_id = created.get("id")
        if not isinstance(batch_id, str):
            raise bench.HarnessError("OpenAI Batch id 누락")
        state = {"batch_id": batch_id, "input_file_id": input_file_id,
                 "offset": args.offset, "limit": args.limit, "reservation_usd": reservation,
                 "status": created.get("status"), "created_at": created.get("created_at")}
        write_state(path, state)
        print(f"BATCH_SUBMITTED offset={args.offset} limit={args.limit} status={state['status']}")
        return 2

    state = json.loads(path.read_text(encoding="utf-8"))
    batch = request_json(api_key, "GET", f"{bench.OPENAI_BATCHES_URL}/{state['batch_id']}")
    state.update({key: batch.get(key) for key in (
        "status", "output_file_id", "error_file_id", "created_at", "completed_at", "request_counts"
    )})
    write_state(path, state)
    status = str(state.get("status", "")).lower()
    if status not in TERMINAL:
        print(f"BATCH_PENDING offset={args.offset} limit={args.limit} status={status}")
        return 2
    if status != "completed" or not state.get("output_file_id"):
        collect(api_key, args, state, selected, joined)
        state["collected"] = True
        write_state(path, state)
        raise bench.HarnessError(
            f"OpenAI Batch terminal failure: {status}; FAILED dense rows preserved"
        )
    collect(api_key, args, state, selected, joined)
    state["collected"] = True
    write_state(path, state)
    print(f"BATCH_COLLECTED offset={args.offset} limit={args.limit}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except bench.HarnessError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
