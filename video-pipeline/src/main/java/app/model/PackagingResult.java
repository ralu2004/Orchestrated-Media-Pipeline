package app.model;

import java.util.List;

/** Outputs of packaging: manifest location and DRM playlist assets. */
public record PackagingResult(String manifestPath, List<String> encryptedAssets) {}