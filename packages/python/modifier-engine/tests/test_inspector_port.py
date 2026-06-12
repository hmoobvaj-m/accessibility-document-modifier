from pathlib import Path

import pytest

from modifier_engine.domain import InspectionResult
from modifier_engine.ports import PDFInspector

class FakePDFInspector:
    def __init__(self, result: InspectionResult) -> None:
        self.result = result
        self.received_path: Path | None = None

    async def inspect(self, source_path: Path) -> InspectionResult:
        self.received_path = source_path
        return self.result


@pytest.fixture
def inspection_result() -> InspectionResult:
    return InspectionResult.model_validate(
        {
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
    )


@pytest.mark.asyncio
async def test_fake_inspector_satisfies_port(inspection_result: InspectionResult,) -> None:
    inspector = FakePDFInspector(inspection_result)
    source_path = Path("/tmp/sample.pdf")

    assert isinstance(inspector, PDFInspector)

    result = await inspector.inspect(source_path)

    assert result == inspection_result
    assert inspector.received_path == source_path