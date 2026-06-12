from __future__ import annotations

from typing import Literal, Self

from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
    field_validator,
    model_validator,
)

class DomainModel(BaseModel):
    """ Base config for modifier-engine domain models """
    model_config = ConfigDict(extra="forbid")

class SourceDocument(DomainModel):
    """ Metadata identifying inspected source pdf """
    file_name: str = Field(min_length=1)
    size_bytes: int = Field(gt=0)
    sha256: str = Field(pattern=r"^[0-9a-f]{64}$")
    

class BoundingBox(DomainModel):
    """Rectangular region measured in PDF points """
    x: float = Field(gt=0)
    y: float = Field(gt=0)
    width: float = Field(gt=0)
    height: float = Field(gt=0)

class StructureTag(DomainModel):
    """ PDF struct tree info associated with a fig """
    tag_name: str = Field(min_length=1)
    structure_path: list[str] = Field(default_factory=list)
    marked_content_id: int | None = Field(default=None, ge=0)

class FigureInspection(DomainModel):
    """ Inspection ddata for single detected fig or image """
    figure_id: str = Field(min_length=1)
    bounding_box: BoundingBox
    alt_text: str | None = None
    is_decorative: bool | None = None
    structure_tag: StructureTag | None = None
    
    @field_validator("alt_text")
    @classmethod
    def normalize_alt_text(cls, value: str | None) -> str | None:
        if value is None:
            return None
        
        normalized = value.strip()
        if not normalized:
            raise ValueError("alt_text can not be blank")
        
        return normalized
    
    @model_validator(mode="after")
    def validate_decorative_figure(self) -> Self:
        if self.is_decorative is True and self.alt_text is not None:
            raise ValueError("decorative figures must not contain alt text")
        
        return self
    
class PageInspection(DomainModel):
    """ Inspection data for single page of source pdf """
    page_number: int = Field(gt=0)
    width_points: float = Field(gt=0)
    height_points: float = Field(gt=0)
    figures: list[FigureInspection] = Field(default_factory=list)
    
    @model_validator(mode="after")
    def validate_figure_bounds(self) -> Self:
        for figure in self.figures:
            bounds = figure.bounding_box
            if bounds.x + bounds.width > self.width_points:
                raise ValueError(f"figure {figure.figure_id!r} extends beyond the page width")
            
            if bounds.y + bounds.height > self.height_points:
                raise ValueError(f"figure {figure.figure_id!r} extends beyond the page height")
            
        return self
    
class InspectionWarning(DomainModel):
    """ Nonfatal issue discovery during pdf inspection """
    code: str = Field(min_length=1)
    message: str = Field(min_length=1)
    severity: Literal["info", "warning", "error"]
    page_number: int | None = Field(default=None, gt=0)
    figure_id: str | None = Field(default=None, min_length=1)

class InspectionResult(DomainModel):
    """ Version result returne by pdf inspector adapter """
    schema_version: Literal["1.0"]
    coordinate_system: Literal["pdf_points_top_left"]
    source: SourceDocument
    page_count: int = Field(gt=0)
    pages: list[PageInspection]
    warnings: list[InspectionWarning] = Field(default_factory=list)
    
    @model_validator(mode="after")
    def validate_document_consistency(self) -> Self:
        self._validate_page_sequence()
        figure_ids = self._collect_figure_ids()
        self._validate_unique_figure_ids(figure_ids)
        self._validate_warning_references(figure_ids)
        
        return self
    
    def _validate_page_sequence(self) -> None:
        expected = list(range(1, self.page_count+1))
        actual = [page.page_number for page in self.pages]
        if actual != expected:
            raise ValueError("Pages must contain exactly one entry for every page in ascending order")
    
    def _collect_figure_ids(self) -> list[str]:
        return [
            figure.figure_id
            for page in self.pages
            for figure in page.figures
        ]
        
    @staticmethod
    def _validate_unique_figure_ids(figure_ids: list[str],) -> None:
        if len(figure_ids) != len(set(figure_ids)):
            raise ValueError("figure_id values must be unique within an inspection result")
        
    def _validate_warning_references(self, figure_ids: list[str],) -> None:
        known_figure_ids = set(figure_ids)
        for warning in self.warnings:
            if(warning.page_number is not None and warning.page_number > self.page_count):
                raise ValueError(f"Warning {warning.code!r} references a page outside the doc")
            
            if(warning.figure_id is not None and warning.figure_id not in known_figure_ids):
                raise ValueError(f"Warning {warning.code!r} references an unknown figure")
        
    
    
        