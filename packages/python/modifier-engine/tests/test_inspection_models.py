from copy import deepcopy
from typing import Any

import pytest
from pydantic import ValidationError

from modifier_engine.domain import InspectionResult

@pytest.fixture
def valid_payload() -> dict[str, Any]:
    return {
        "schema_version": "1.0",
        "coordinate_system": "pdf_points_top_left",
        "source": {
            "file_name": "training-guide.pdf",
            "size_bytes": 4096,
            "sha256": "a" * 64, 
        },
        "page_count": 2,
        "pages": [
            {
                "page_number": 1,
                "width_points": 612.0,
                "height_points": 792.0,
                "figures": [
                    {
                        "figure_id": "figure-1",
                        "bounding_box": {
                            "x": 72.0,
                            "y": 96.0,
                            "width": 240.0,
                            "height": 180.0,
                        },
                        "alt_text": ("SAP Fiori application overview."),
                        "is_decorative": False,
                        "structure_tag": {
                            "tag_name": "Figure",
                            "structure_path": [
                                "Document",
                                "Sect",
                                "Figure",
                            ],
                            "marked_content_id": 1,
                        },
                    }
                ],
            },
            {
                "page_number": 2,
                "width_points": 612.0,
                "height_points": 792.0,
                "figures": [],
            },
        ],
        "warnings": [
            {
                "code": "UNTAGGED_IMAGE",
                "message": "An image is not associated with a structure element",
                "severity": "warning",
                "page_number": 2,
                "figure_id": None,
            }
        ],
    }

def test_parses_valid_inspection_result(
    valid_payload: dict[str, Any],
) -> None:
    result = InspectionResult.model_validate(valid_payload)

    assert result.schema_version == "1.0"
    assert result.page_count == 2
    assert len(result.pages) == 2

    first_figure = result.pages[0].figures[0]

    assert first_figure.figure_id == "figure-1"
    assert first_figure.alt_text == (valid_payload["pages"][0]["figures"][0]["alt_text"])
    assert first_figure.structure_tag is not None
    assert first_figure.structure_tag.tag_name == "Figure"


def test_serializes_to_original_json_shape(
    valid_payload: dict[str, Any],
) -> None:
    result = InspectionResult.model_validate(valid_payload)

    assert result.model_dump(mode="json") == valid_payload


def test_rejects_missing_required_source(
    valid_payload: dict[str, Any],
) -> None:
    payload = deepcopy(valid_payload)
    del payload["source"]

    with pytest.raises(ValidationError):
        InspectionResult.model_validate(payload)


@pytest.mark.parametrize(
    ("field_path", "invalid_value"),
    [
        (("pages", 0, "page_number"), 0),
        (("pages", 0, "width_points"), 0),
        (("pages", 0, "height_points"), -1),
    ],
)
def test_rejects_invalid_page_values(
    valid_payload: dict[str, Any],
    field_path: tuple[str | int, ...],
    invalid_value: int,
) -> None:
    payload = deepcopy(valid_payload)
    target: Any = payload

    for path_component in field_path[:-1]:
        target = target[path_component]

    target[field_path[-1]] = invalid_value

    with pytest.raises(ValidationError):
        InspectionResult.model_validate(payload)


def test_rejects_page_count_mismatch(
    valid_payload: dict[str, Any],
) -> None:
    payload = deepcopy(valid_payload)
    payload["page_count"] = 3

    with pytest.raises(
        ValidationError,
        match="exactly one entry for every page",
    ):
        InspectionResult.model_validate(payload)


def test_rejects_unknown_schema_version(
    valid_payload: dict[str, Any],
) -> None:
    payload = deepcopy(valid_payload)
    payload["schema_version"] = "2.0"

    with pytest.raises(ValidationError):
        InspectionResult.model_validate(payload)


def test_rejects_figure_outside_page_width(
    valid_payload: dict[str, Any],
) -> None:
    payload = deepcopy(valid_payload)
    bounding_box = payload["pages"][0]["figures"][0][
        "bounding_box"
    ]
    bounding_box["width"] = 1000.0

    with pytest.raises(
        ValidationError,
        match="extends beyond the page width",
    ):
        InspectionResult.model_validate(payload)


def test_rejects_alt_text_for_decorative_figure(
    valid_payload: dict[str, Any],
) -> None:
    payload = deepcopy(valid_payload)
    figure = payload["pages"][0]["figures"][0]
    figure["is_decorative"] = True

    with pytest.raises(
        ValidationError,
        match="decorative figures must not contain alt text",
    ):
        InspectionResult.model_validate(payload)


def test_rejects_duplicate_figure_ids(
    valid_payload: dict[str, Any],
) -> None:
    payload = deepcopy(valid_payload)
    duplicate = deepcopy(
        payload["pages"][0]["figures"][0]
    )

    payload["pages"][1]["figures"].append(duplicate)

    with pytest.raises(
        ValidationError,
        match="figure_id values must be unique",
    ):
        InspectionResult.model_validate(payload)


def test_rejects_warning_for_unknown_figure(
    valid_payload: dict[str, Any],
) -> None:
    payload = deepcopy(valid_payload)
    payload["warnings"][0]["figure_id"] = "figure-999"

    with pytest.raises(
        ValidationError,
        match="references an unknown figure",
    ):
        InspectionResult.model_validate(payload)


def test_rejects_unknown_fields(
    valid_payload: dict[str, Any],
) -> None:
    payload = deepcopy(valid_payload)
    payload["unexpected"] = "value"

    with pytest.raises(ValidationError):
        InspectionResult.model_validate(payload)