#!/usr/bin/env python3
"""Count lines in Java and XML files."""

import os
from pathlib import Path


def count_lines(extensions: list[str]) -> dict:
    """Count files and lines for given extensions."""
    result = {}
    for ext in extensions:
        files = list(Path(".").rglob(f"*{ext}"))
        total_lines = 0
        for f in files:
            try:
                with open(f, "r", encoding="utf-8", errors="ignore") as fp:
                    total_lines += sum(1 for _ in fp)
            except:
                pass
        result[ext] = {"files": len(files), "lines": total_lines}
    return result


if __name__ == "__main__":
    print("=== File Line Count Statistics ===\n")

    stats = count_lines([".java", ".xml"])

    total_files = 0
    total_lines = 0

    for ext, data in stats.items():
        print(f"{ext[1:].upper():3} files: {data['files']:4} files, {data['lines']:6} lines")
        total_files += data["files"]
        total_lines += data["lines"]

    print(f"\n=== Total: {total_files} files, {total_lines} lines ===")
