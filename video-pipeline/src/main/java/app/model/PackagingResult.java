package app.model;

import java.util.List;

public record PackagingResult(String manifestPath, List<String> encryptedAssets) {}