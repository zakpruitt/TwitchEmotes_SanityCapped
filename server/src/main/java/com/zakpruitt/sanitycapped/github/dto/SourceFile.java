package com.zakpruitt.sanitycapped.github.dto;

/** One file in the repo's tools/source/. */
public record SourceFile(String name, String sha, String downloadUrl) {
}
