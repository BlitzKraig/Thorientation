#!/usr/bin/env python
import sys
import os
import re

def main():
    if len(sys.argv) < 2:
        print("Usage: extract_changelog.py <version_or_tag> [changelog_path] [output_path]")
        sys.exit(1)

    raw_version = sys.argv[1].strip()
    version = raw_version.lstrip("v")
    changelog_path = sys.argv[2] if len(sys.argv) > 2 else "CHANGELOG.md"
    output_path = sys.argv[3] if len(sys.argv) > 3 else "RELEASE_NOTES.md"

    notes = ""
    if os.path.exists(changelog_path):
        with open(changelog_path, "r") as f:
            content = f.read()

        # Match section starting with ## [version] or ## version until next ## or end of file
        escaped_version = re.escape(version)
        pattern = r"##\s+\[?" + escaped_version + r"\]?.*?(?=\n##\s+|\Z)"
        match = re.search(pattern, content, re.DOTALL)
        if match:
            notes = match.group(0).strip()

    if not notes:
        notes = "## Thorientation v" + version + "\n\nSee CHANGELOG.md for detailed release notes."

    with open(output_path, "w") as f:
        f.write(notes + "\n")

    print("Successfully extracted notes for version " + version + " to " + output_path)

if __name__ == "__main__":
    main()
