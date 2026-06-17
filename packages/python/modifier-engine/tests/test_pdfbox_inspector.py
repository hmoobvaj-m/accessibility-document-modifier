from __future__ import annotations

import asyncio
import json
from pathlib import Path
from typing import Any

import pytest

import modifier_engine.adapters.pdfbox as pdfbox_module
from modifier_engine.adapters import (
    PDFBoxExecutableNotFoundError,
    PDFBoxInspector,
    PDFBoxOutputError,
    PDFBoxProcessError,
    PDFBoxTimeoutError,
)
from modifier_engine.domain import InspectionResult
from modifier_engine.ports import PDFInspector


class FakeProcess:
    def __init__(
        self,
        *,
        stdout: bytes = b"",
        stderr: bytes = b"",
        returncode: int = 0,
    ) -> None:
        self.stdout = stdout
        self.stderr = stderr
        self.returncode = returncode
        self.killed = False
        self.communicate_calls = 0

    async def communicate(
        self,
    ) -> tuple[bytes, bytes]:
        self.communicate_calls += 1
        return self.stdout, self.stderr

    def kill(self) -> None:
        self.killed = True


class TimeoutProcess(FakeProcess):
    async def communicate(
        self,
    ) -> tuple[bytes, bytes]:
        self.communicate_calls += 1

        if self.communicate_calls == 1:
            await asyncio.Event().wait()

        return self.stdout, self.stderr


@pytest.fixture
def valid_payload() -> dict[str, Any]:
    return {
        "schema_version": "1.0",
        "coordinate_system": "pdf_points_top_left",
        "source": {
            "file_name": "sample.pdf",
            "size_bytes": 1024,
            "sha256": "a" * 64,
        },
        "page_count": 1,
        "pages": [
            {
                "page_number": 1,
                "width_points": 612.0,
                "height_points": 792.0,
                "figures": [],
            }
        ],
        "warnings": [],
    }


def _create_input_files(
    tmp_path: Path,
) -> tuple[Path, Path]:
    source_path = tmp_path / "sample document.pdf"
    jar_path = tmp_path / "pdf inspector.jar"

    source_path.write_bytes(b"%PDF-1.7\n")
    jar_path.write_bytes(b"fake jar")

    return source_path, jar_path


def _patch_subprocess(
    monkeypatch: pytest.MonkeyPatch,
    process: FakeProcess,
) -> list[
    tuple[
        tuple[str, ...],
        dict[str, object],
    ]
]:
    calls: list[
        tuple[
            tuple[str, ...],
            dict[str, object],
        ]
    ] = []

    async def fake_create_subprocess_exec(
        *args: str,
        **kwargs: object,
    ) -> FakeProcess:
        calls.append((args, kwargs))
        return process

    monkeypatch.setattr(
        pdfbox_module.asyncio,
        "create_subprocess_exec",
        fake_create_subprocess_exec,
    )

    return calls


@pytest.mark.asyncio
async def test_inspect_returns_valid_result_and_uses_direct_arguments(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
    valid_payload: dict[str, Any],
) -> None:
    source_path, jar_path = _create_input_files(
        tmp_path
    )
    process = FakeProcess(
        stdout=json.dumps(valid_payload).encode("utf-8")
    )
    calls = _patch_subprocess(
        monkeypatch,
        process,
    )
    inspector = PDFBoxInspector(
        jar_path=jar_path,
        java_executable="custom-java",
    )

    assert isinstance(inspector, PDFInspector)

    result = await inspector.inspect(source_path)

    assert result == InspectionResult.model_validate(
        valid_payload
    )
    assert calls == [
        (
            (
                "custom-java",
                "-jar",
                str(jar_path),
                "inspect",
                str(source_path),
            ),
            {
                "stdout": asyncio.subprocess.PIPE,
                "stderr": asyncio.subprocess.PIPE,
            },
        )
    ]


@pytest.mark.asyncio
async def test_missing_source_fails_before_process_creation(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    jar_path = tmp_path / "inspector.jar"
    jar_path.write_bytes(b"fake jar")

    process = FakeProcess()
    calls = _patch_subprocess(
        monkeypatch,
        process,
    )
    inspector = PDFBoxInspector(
        jar_path=jar_path
    )

    with pytest.raises(
        FileNotFoundError,
        match="source PDF not found",
    ):
        await inspector.inspect(
            tmp_path / "missing.pdf"
        )

    assert calls == []


@pytest.mark.asyncio
async def test_missing_jar_fails_before_process_creation(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    source_path = tmp_path / "sample.pdf"
    source_path.write_bytes(b"%PDF-1.7\n")

    process = FakeProcess()
    calls = _patch_subprocess(
        monkeypatch,
        process,
    )
    inspector = PDFBoxInspector(
        jar_path=tmp_path / "missing.jar"
    )

    with pytest.raises(
        FileNotFoundError,
        match="PDFBox inspector JAR not found",
    ):
        await inspector.inspect(source_path)

    assert calls == []


@pytest.mark.asyncio
async def test_missing_java_executable_raises_adapter_error(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    source_path, jar_path = _create_input_files(
        tmp_path
    )

    async def raise_missing_executable(
        *args: str,
        **kwargs: object,
    ) -> FakeProcess:
        raise FileNotFoundError("java")

    monkeypatch.setattr(
        pdfbox_module.asyncio,
        "create_subprocess_exec",
        raise_missing_executable,
    )
    inspector = PDFBoxInspector(
        jar_path=jar_path,
        java_executable="missing-java",
    )

    with pytest.raises(
        PDFBoxExecutableNotFoundError
    ) as exc_info:
        await inspector.inspect(source_path)

    assert (
        exc_info.value.executable
        == "missing-java"
    )


@pytest.mark.asyncio
async def test_nonzero_exit_raises_process_error(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    source_path, jar_path = _create_input_files(
        tmp_path
    )
    process = FakeProcess(
        stderr=b"inspection failed",
        returncode=7,
    )
    _patch_subprocess(
        monkeypatch,
        process,
    )
    inspector = PDFBoxInspector(
        jar_path=jar_path
    )

    with pytest.raises(
        PDFBoxProcessError
    ) as exc_info:
        await inspector.inspect(source_path)

    assert exc_info.value.returncode == 7
    assert (
        exc_info.value.stderr
        == "inspection failed"
    )


@pytest.mark.asyncio
async def test_timeout_kills_and_reaps_process(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    source_path, jar_path = _create_input_files(
        tmp_path
    )
    process = TimeoutProcess()
    _patch_subprocess(
        monkeypatch,
        process,
    )
    inspector = PDFBoxInspector(
        jar_path=jar_path,
        timeout_seconds=0.001,
    )

    with pytest.raises(PDFBoxTimeoutError):
        await inspector.inspect(source_path)

    assert process.killed is True
    assert process.communicate_calls == 2


@pytest.mark.asyncio
async def test_malformed_json_raises_output_error(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    source_path, jar_path = _create_input_files(
        tmp_path
    )
    process = FakeProcess(
        stdout=b"{not-json"
    )
    _patch_subprocess(
        monkeypatch,
        process,
    )
    inspector = PDFBoxInspector(
        jar_path=jar_path
    )

    with pytest.raises(
        PDFBoxOutputError,
        match="invalid inspection JSON",
    ):
        await inspector.inspect(source_path)


@pytest.mark.asyncio
async def test_schema_invalid_json_raises_output_error(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
    valid_payload: dict[str, Any],
) -> None:
    source_path, jar_path = _create_input_files(
        tmp_path
    )

    invalid_payload = dict(valid_payload)
    invalid_payload["schema_version"] = "2.0"

    process = FakeProcess(
        stdout=json.dumps(
            invalid_payload
        ).encode("utf-8")
    )
    _patch_subprocess(
        monkeypatch,
        process,
    )
    inspector = PDFBoxInspector(
        jar_path=jar_path
    )

    with pytest.raises(
        PDFBoxOutputError,
        match="invalid inspection JSON",
    ):
        await inspector.inspect(source_path)