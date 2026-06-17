from __future__ import annotations

import asyncio
from pathlib import Path

from pydantic import ValidationError

from modifier_engine.domain import InspectionResult


class PDFBoxInspectorError(RuntimeError):
    """Base exception for PDFBox inspector adapter failures."""

class PDFBoxExecutableNotFoundError(PDFBoxInspectorError):
    """Raised when the configured Java executable cannot be started."""
    def __init__(self, executable: str) -> None:
        self.executable = executable
        super().__init__(f"Java executable not found: {executable}")


class PDFBoxProcessError(PDFBoxInspectorError):
    """Raised when the PDFBox process exits unsuccessfully."""
    def __init__(self, returncode: int, stderr: str) -> None:
        self.returncode = returncode
        self.stderr = stderr
        message = f"PDFBox inspector exited with status {returncode}"

        if stderr:
            message = f"{message}: {stderr}"

        super().__init__(message)


class PDFBoxTimeoutError(PDFBoxInspectorError):
    """Raised when the PDFBox process exceeds its configured timeout."""
    def __init__(self, timeout_seconds: float) -> None:
        self.timeout_seconds = timeout_seconds
        super().__init__(
            f"PDFBox inspector exceeded the "
            f"{timeout_seconds:g}-second timeout"
        )


class PDFBoxOutputError(PDFBoxInspectorError):
    """Raised when PDFBox returns invalid inspection output."""

class PDFBoxInspector:
    """Run the Java PDFBox inspector and validate its JSON response."""
    def __init__(self, jar_path: Path, java_executable: str = "java", timeout_seconds: float = 30.0) -> None:
        if not java_executable.strip():
            raise ValueError("java_executable must not be blank")

        if timeout_seconds <= 0:
            raise ValueError("timeout_seconds must be greater than zero")

        self._jar_path = jar_path
        self._java_executable = java_executable
        self._timeout_seconds = timeout_seconds

    async def inspect(self, source_path: Path) -> InspectionResult:
        """Inspect a source PDF using the configured Java CLI."""
        self._require_file(source_path, "source PDF")
        self._require_file(self._jar_path, "PDFBox inspector JAR")

        try:
            process = await asyncio.create_subprocess_exec(
                self._java_executable,
                "-jar",
                str(self._jar_path),
                "inspect",
                str(source_path),
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )
        except FileNotFoundError as exc:
            raise PDFBoxExecutableNotFoundError(self._java_executable) from exc

        try:
            stdout, stderr = await asyncio.wait_for(process.communicate(), timeout=self._timeout_seconds)
            
        except TimeoutError as exc:
            try:
                process.kill()
            except ProcessLookupError:
                # The process may have exited between the timeout and attempted termination.
                pass

            # Reap the process and drain its output pipes.
            await process.communicate()

            raise PDFBoxTimeoutError(self._timeout_seconds) from exc

        stderr_text = stderr.decode("utf-8", errors="replace").strip()

        if process.returncode != 0:
            raise PDFBoxProcessError(process.returncode, stderr_text)

        try:
            stdout_text = stdout.decode("utf-8")
            
        except UnicodeDecodeError as exc:
            raise PDFBoxOutputError("PDFBox inspector output was not valid UTF-8") from exc

        if not stdout_text.strip():
            raise PDFBoxOutputError("PDFBox inspector returned no inspection JSON")

        try:
            return InspectionResult.model_validate_json(stdout_text)
        
        except ValidationError as exc:
            raise PDFBoxOutputError("PDFBox inspector returned invalid inspection JSON") from exc

    @staticmethod
    def _require_file(path: Path, description: str) -> None:
        if not path.is_file():
            raise FileNotFoundError(f"{description} not found: {path}")