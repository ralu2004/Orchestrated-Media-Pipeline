package app.services.packaging;

import app.common.PipelineStage;
import app.model.PackagingContext;
import app.model.PackagingResult;

public interface PackagingService extends PipelineStage<PackagingContext, PackagingResult> {
}
