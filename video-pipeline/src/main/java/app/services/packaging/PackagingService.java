package app.services.packaging;

import app.common.PipelineStage;
import app.model.PackagingContext;
import app.model.PackagingResult;

/** Stage contract for producing final delivery artifacts and manifest metadata. */
public interface PackagingService extends PipelineStage<PackagingContext, PackagingResult> {
}
