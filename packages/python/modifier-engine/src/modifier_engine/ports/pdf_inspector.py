from pathlib import Path
from typing import Protocol, runtime_checkable

from modifier_engine.domain import InspectionResult

@runtime_checkable
class PDFInspector(Protocol):
    """ Port implemented by services capable of inspecting PDF docs """
    async def inspect(self, source_path: Path) -> InspectionResult:
        """ Inspect PDF and return normnalized inspection result """
        ...