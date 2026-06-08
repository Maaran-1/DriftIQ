from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user_dep
from app.core.database import get_db
from app.ml.baseline_model import build_dimensions_from_dict
from app.models.digital_twin import DigitalTwin
from app.models.user import User
from app.schemas.twin import BaselineDimensionOut, TwinResetResponse, TwinStatusResponse
from app.services.twin_manager import get_or_create_twin, reset_twin

router = APIRouter(prefix="/twin", tags=["twin"])


@router.get("/status", response_model=TwinStatusResponse)
async def get_twin_status(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    twin = await get_or_create_twin(db, current_user.id)
    dims = build_dimensions_from_dict(twin.dimensions or {})
    dimensions_out = {
        name: BaselineDimensionOut(
            weekday_mean=d.weekday_mean,
            weekday_std=d.weekday_std,
            weekend_mean=d.weekend_mean,
            weekend_std=d.weekend_std,
            confidence=d.confidence,
            n_samples=d.n_samples,
        )
        for name, d in dims.items()
    }
    calibration_progress = min(twin.baseline_days / 14.0, 1.0)
    return TwinStatusResponse(
        is_active=twin.is_active,
        baseline_days=twin.baseline_days,
        confidence_score=twin.confidence_score,
        dimensions=dimensions_out,
        calibration_progress=calibration_progress,
    )


@router.post("/reset", response_model=TwinResetResponse)
async def reset_twin_endpoint(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user_dep),
):
    await reset_twin(db, current_user.id, reason="user_request")
    return TwinResetResponse(message="Baseline reset initiated. Recalibration will take 7–14 days.")
